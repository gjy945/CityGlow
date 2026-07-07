package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenMeteoResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ForecastService 单元测试。
 *
 * <p>用 Mockito mock {@link OpenMeteoClient},不真实调用 Open-Meteo API。
 * 月相由 {@link com.cityglow.util.MoonPhaseCalculator} 根据当前日期计算,
 * 测试只验证云量/Bortle 对 score 的影响(月相是确定性算法,无法 mock)。</p>
 *
 * <p>注入真实 Caffeine 缓存实例,验证缓存命中时 OpenMeteoClient 不被重复调用。</p>
 */
@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private OpenMeteoClient openMeteoClient;

    private ForecastService forecastService;

    /** 真实 Caffeine 缓存,用于验证缓存命中行为。 */
    private Cache<String, ForecastResult> forecastCache;

    @BeforeEach
    void setUp() {
        forecastCache = Caffeine.newBuilder().build();
        forecastService = new ForecastService(openMeteoClient, forecastCache);
    }

    /**
     * 北京 (39.9, 116.4) → Bortle 5。
     * 云量 10(≤20 不扣分)。
     * score = 100 - 0 - moon*30 - 32 = 68 - moon*30,moon∈[0,1] → score∈[38, 68]。
     */
    @Test
    void forecast_beijing_lowCloudCover_scoreInRange38To68() {
        double lat = 39.9;
        double lng = 116.4;

        OpenMeteoResponse response = new OpenMeteoResponse(
                new OpenMeteoResponse.Current(10, 15.0, 60, 0),
                new OpenMeteoResponse.Daily(
                        List.of("2026-07-07T04:53"), List.of("2026-07-07T19:45")));

        when(openMeteoClient.getForecast(lat, lng)).thenReturn(response);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.cloudCover()).isEqualTo(10.0);
        assertThat(result.bortleLevel()).isEqualTo(5);
        assertThat(result.score()).isBetween(38, 68);
        assertThat(result.sunrise()).isGreaterThan(0);
        assertThat(result.sunset()).isGreaterThan(0);
    }

    /**
     * 阿里天文台 (32.5, 80.0) → Bortle 1(极佳暗空)。
     * 云量 0 + Bortle 1 → score = 100 - moon*30,moon∈[0,1] → score∈[70, 100]。
     */
    @Test
    void forecast_aliObservatory_bortle1_scoreInRange70To100() {
        double lat = 32.5;
        double lng = 80.0;

        OpenMeteoResponse response = new OpenMeteoResponse(
                new OpenMeteoResponse.Current(0, 5.0, 40, 0),
                null);

        when(openMeteoClient.getForecast(lat, lng)).thenReturn(response);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.bortleLevel()).isEqualTo(1);
        assertThat(result.score()).isBetween(70, 100);
    }

    /**
     * null 响应兜底:OpenMeteoClient 返回 null。
     * cloudCover 兜底 100 → score clamp 到 0。
     */
    @Test
    void forecast_nullResponse_fallbackCloudCover100_scoreClampedTo0() {
        double lat = 39.9;
        double lng = 116.4;

        when(openMeteoClient.getForecast(lat, lng)).thenReturn(null);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.cloudCover()).isEqualTo(100.0);
        assertThat(result.bortleLevel()).isEqualTo(5);
        assertThat(result.message()).isEqualTo("不建议");
        assertThat(result.sunrise()).isEqualTo(0L);
        assertThat(result.sunset()).isEqualTo(0L);
    }

    /**
     * current 为 null 但 daily 有数据时,云量兜底 100,sunrise/sunset 正常读取。
     */
    @Test
    void forecast_nullCurrent_dailyStillParsed() {
        double lat = 39.9;
        double lng = 116.4;

        OpenMeteoResponse response = new OpenMeteoResponse(
                null,
                new OpenMeteoResponse.Daily(
                        List.of("2026-07-07T04:53"), List.of("2026-07-07T19:45")));

        when(openMeteoClient.getForecast(lat, lng)).thenReturn(response);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.cloudCover()).isEqualTo(100.0);
        assertThat(result.sunrise()).isGreaterThan(0);
        assertThat(result.sunset()).isGreaterThan(0);
    }

    /**
     * 缓存验证:同一 (lat,lng) 第二次调用应命中缓存,OpenMeteoClient 不再被调用。
     *
     * <p>验证 forecastCache 命中行为:</p>
     * <ul>
     *   <li>第一次调用 OpenMeteoClient 被调 1 次,结果写入缓存。</li>
     *   <li>第二次调用缓存命中,OpenMeteoClient 调用次数仍为 1。</li>
     *   <li>两次返回的 ForecastResult 引用相同(同一缓存对象)。</li>
     * </ul>
     */
    @Test
    void forecast_secondCall_hitsCacheWithoutInvokingClient() {
        double lat = 39.9;
        double lng = 116.4;

        OpenMeteoResponse response = new OpenMeteoResponse(
                new OpenMeteoResponse.Current(10, 15.0, 60, 0),
                new OpenMeteoResponse.Daily(
                        List.of("2026-07-07T04:53"), List.of("2026-07-07T19:45")));

        when(openMeteoClient.getForecast(lat, lng)).thenReturn(response);

        // 第一次调用 → cache miss → 调 OpenMeteoClient 1 次
        ForecastResult first = forecastService.forecast(lat, lng);
        // 第二次调用 → cache hit → 不再调 OpenMeteoClient
        ForecastResult second = forecastService.forecast(lat, lng);

        // 验证 OpenMeteoClient 仅被调 1 次(缓存命中跳过回源)
        verify(openMeteoClient, times(1)).getForecast(lat, lng);
        // 缓存命中返回同一对象引用
        assertThat(second).isSameAs(first);
    }
}
