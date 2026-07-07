package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenWeatherCurrentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ForecastService 单元测试。
 *
 * <p>用 Mockito mock {@link OpenWeatherClient},不真实调用 OpenWeatherMap API。
 * 月相由 {@link com.cityglow.util.MoonPhaseCalculator} 根据当前日期计算,
 * 测试只验证云量/Bortle 对 score 的影响(月相是确定性算法,无法 mock)。</p>
 *
 * <p>验证场景:</p>
 * <ul>
 *   <li>北京低云量:云量 10(≤20 不扣分),Bortle 5 → score 受月相影响在合理范围</li>
 *   <li>阿里天文台:云量 0 + Bortle 1 → score 仅受月相影响(≥70)</li>
 *   <li>null 响应兜底:云量 100 → score clamp 到低值</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private OpenWeatherClient openWeatherClient;

    @InjectMocks
    private ForecastService forecastService;

    /**
     * 北京 (39.9, 116.4) → Bortle 5。
     * 云量 10(≤20 不扣分)。
     * 月相由天文算法计算,照亮比例 0-1,score = 100 - 0 - moon*30 - 32 = 68 - moon*30。
     * moon∈[0,1] → score∈[38, 68]。
     */
    @Test
    void forecast_beijing_lowCloudCover_scoreInRange38To68() {
        double lat = 39.9;
        double lng = 116.4;

        OpenWeatherCurrentResponse current =
                new OpenWeatherCurrentResponse(
                        new OpenWeatherCurrentResponse.Clouds(10),
                        null, null, null);

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(current);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.cloudCover()).isEqualTo(10.0);
        assertThat(result.bortleLevel()).isEqualTo(5);
        assertThat(result.score()).isBetween(38, 68);
    }

    /**
     * 阿里天文台 (32.5, 80.0) → Bortle 1(极佳暗空)。
     * 云量 0 + Bortle 1 → score = 100 - 0 - moon*30 - 0 = 100 - moon*30。
     * moon∈[0,1] → score∈[70, 100]。
     */
    @Test
    void forecast_aliObservatory_bortle1_scoreInRange70To100() {
        double lat = 32.5;
        double lng = 80.0;

        OpenWeatherCurrentResponse current =
                new OpenWeatherCurrentResponse(
                        new OpenWeatherCurrentResponse.Clouds(0),
                        null, null, null);

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(current);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.bortleLevel()).isEqualTo(1);
        assertThat(result.score()).isBetween(70, 100);
    }

    /**
     * null 响应兜底:OpenWeatherClient 返回 null。
     * cloudCover 兜底 100 → score = 100 - (100-20)*1.5 - moon*30 - 32 = -52 - moon*30 → clamp 0。
     */
    @Test
    void forecast_nullResponse_fallbackCloudCover100_scoreClampedTo0() {
        double lat = 39.9;
        double lng = 116.4;

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(null);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.cloudCover()).isEqualTo(100.0);
        assertThat(result.bortleLevel()).isEqualTo(5);
        assertThat(result.message()).isEqualTo("不建议");
        assertThat(result.sunrise()).isEqualTo(0L);
        assertThat(result.sunset()).isEqualTo(0L);
    }

    /**
     * 验证 sunrise/sunset 从 /weather 响应的 sys 字段正确读取。
     */
    @Test
    void forecast_currentResponseWithSys_extractsSunriseSunset() {
        double lat = 39.9;
        double lng = 116.4;

        OpenWeatherCurrentResponse current =
                new OpenWeatherCurrentResponse(
                        new OpenWeatherCurrentResponse.Clouds(0),
                        null, null,
                        new OpenWeatherCurrentResponse.Sys(1700000000L, 1700050000L));

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(current);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.sunrise()).isEqualTo(1700000000L);
        assertThat(result.sunset()).isEqualTo(1700050000L);
    }
}
