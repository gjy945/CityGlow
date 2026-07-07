package com.cityglow.service;

import com.cityglow.domain.BestWindowResult;
import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenMeteoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * BestWindowService 单元测试。
 *
 * <p>用 Mockito mock {@link ForecastService} 与 {@link OpenMeteoClient},
 * 不真实调用任何外部 API。验证:</p>
 * <ul>
 *   <li>云量 &gt; 60% 时 message = "今晚云量较高,不适合观星",score=0。</li>
 *   <li>云量较低、月相接近新月时,score 较高,message 含"良好"或"极佳"。</li>
 *   <li>暗夜窗口:startTime = sunset + 1.5h,endTime = sunrise - 1.5h(支持跨午夜)。</li>
 *   <li>reasons 列表含 Bortle 与云量信息。</li>
 *   <li>ForecastResult 中 sunrise/sunset 为 0 时,回退调 OpenMeteoClient 取 ISO 字符串。</li>
 * </ul>
 *
 * <p>月相由 {@link com.cityglow.util.MoonPhaseCalculator} 根据当前日期确定性计算,
 * 测试无法 mock,只验证评分范围与窗口结构,不依赖具体月相值。</p>
 */
@ExtendWith(MockitoExtension.class)
class BestWindowServiceTest {

    @Mock
    private ForecastService forecastService;

    @Mock
    private OpenMeteoClient openMeteoClient;

    private BestWindowService bestWindowService;

    @BeforeEach
    void setUp() {
        bestWindowService = new BestWindowService(forecastService, openMeteoClient);
    }

    /**
     * 云量 80%(>60% 阈值)→ score=0,message="今晚云量较高,不适合观星"。
     *
     * <p>验证:云量过高时直接返回不适合观星的判定,reasons 含云量阈值说明。</p>
     */
    @Test
    void getBestWindow_highCloudCover_returnsUnsuitableMessage() {
        double lat = 39.9;
        double lng = 116.4;
        ForecastResult forecast = new ForecastResult(
                20, 80.0, "满月", 5, "郊区天空", "不建议",
                1700000000L, 1700050000L);
        when(forecastService.forecast(lat, lng)).thenReturn(forecast);

        BestWindowResult result = bestWindowService.getBestWindow(lat, lng);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.message()).isEqualTo("今晚云量较高,不适合观星");
        assertThat(result.reasons()).isNotEmpty();
        assertThat(result.reasons().get(0)).contains("云量");
        assertThat(result.reasons().get(0)).contains("60");
    }

    /**
     * 低云量、低 Bortle(阿里天文台)→ score 较高,message 含"良好"或"极佳"。
     *
     * <p>sunset=1700050000 → 2023-11-15T10:46:40 UTC → LocalTime 10:46:40;
     * sunrise=1700000000 → 2023-11-14T22:13:20 UTC → LocalTime 22:13:20。
     * windowStart = sunset + 1.5h ≈ 12:16;windowEnd = sunrise - 1.5h ≈ 20:43。
     * 测试只验证 startTime/endTime 非空且在合理范围。</p>
     */
    @Test
    void getBestWindow_lowCloudCoverLowBortle_returnsHighScore() {
        double lat = 32.5;
        double lng = 80.0;
        ForecastResult forecast = new ForecastResult(
                90, 5.0, "新月", 1, "极佳暗空", "今夜极佳!",
                1700000000L, 1700050000L);
        when(forecastService.forecast(lat, lng)).thenReturn(forecast);

        BestWindowResult result = bestWindowService.getBestWindow(lat, lng);

        // 基础分 90,可能因月亮在窗口内 ±10,clamp 到 [70, 100]
        assertThat(result.score()).isBetween(70, 100);
        assertThat(result.message()).containsAnyOf("今夜极佳", "今夜观测条件良好");
        assertThat(result.startTime()).isNotNull();
        assertThat(result.endTime()).isNotNull();
        // reasons 必含 Bortle 信息
        assertThat(result.reasons().stream().anyMatch(r -> r.contains("Bortle"))).isTrue();
        // 云量低,reasons 含"云量仅"
        assertThat(result.reasons().stream().anyMatch(r -> r.contains("云量仅"))).isTrue();
    }

    /**
     * ForecastResult 的 sunrise/sunset 为 0(Open-Meteo 偶发返回 null daily)时,
     * 回退调 OpenMeteoClient 取 ISO 字符串解析。
     *
     * <p>验证兜底逻辑:resolveSunriseTime/resolveSunsetTime 在 epoch=0 时调 OpenMeteoClient,
     * 用返回的 ISO 字符串("2026-07-07T04:53")解析为 LocalTime。</p>
     */
    @Test
    void getBestWindow_zeroEpoch_fallsBackToOpenMeteoClient() {
        double lat = 39.9;
        double lng = 116.4;
        ForecastResult forecast = new ForecastResult(
                50, 10.0, "上弦月", 5, "郊区天空", "可观测",
                0L, 0L);
        when(forecastService.forecast(lat, lng)).thenReturn(forecast);

        OpenMeteoResponse omResponse = new OpenMeteoResponse(
                new OpenMeteoResponse.Current(10, 15.0, 60, 0),
                new OpenMeteoResponse.Daily(
                        List.of("2026-07-07T04:53"), List.of("2026-07-07T19:45")));
        when(openMeteoClient.getForecast(lat, lng)).thenReturn(omResponse);

        BestWindowResult result = bestWindowService.getBestWindow(lat, lng);

        // 验证 window 计算正常(没有因为 epoch=0 而 NPE)
        assertThat(result).isNotNull();
        assertThat(result.startTime()).isNotNull();
        assertThat(result.endTime()).isNotNull();
        // sunset=19:45 → windowStart = 19:45 + 1.5h = 21:15
        assertThat(result.startTime()).isEqualTo(LocalTime.of(21, 15));
        // sunrise=04:53 → windowEnd = 04:53 - 1.5h = 03:23
        assertThat(result.endTime()).isEqualTo(LocalTime.of(3, 23));
    }

    /**
     * 暗夜窗口跨午夜验证:sunset 早(sunset=17:00,冬季)→ windowStart=18:30,
     * sunrise 晚(sunrise=07:00,冬季)→ windowEnd=05:30。
     *
     * <p>验证 startTime > endTime(跨午夜),且窗口合法。</p>
     */
    @Test
    void getBestWindow_winterTimes_windowSpansMidnight() {
        double lat = 39.9;
        double lng = 116.4;
        // sunset=17:00, sunrise=07:00(冬季典型)
        long sunsetEpoch = LocalDateTime.of(2026, 1, 15, 17, 0)
                .toEpochSecond(ZoneOffset.UTC);
        long sunriseEpoch = LocalDateTime.of(2026, 1, 15, 7, 0)
                .toEpochSecond(ZoneOffset.UTC);
        ForecastResult forecast = new ForecastResult(
                70, 5.0, "新月", 5, "郊区天空", "适合观星",
                sunriseEpoch, sunsetEpoch);
        when(forecastService.forecast(lat, lng)).thenReturn(forecast);

        BestWindowResult result = bestWindowService.getBestWindow(lat, lng);

        // windowStart = 17:00 + 1.5h = 18:30
        assertThat(result.startTime()).isEqualTo(LocalTime.of(18, 30));
        // windowEnd = 07:00 - 1.5h = 05:30
        assertThat(result.endTime()).isEqualTo(LocalTime.of(5, 30));
        // 跨午夜:startTime > endTime
        assertThat(result.startTime()).isAfter(result.endTime());
    }

    /**
     * 阿里天文台 Bortle 1 极佳暗空 → reasons 含 "Bortle 1"。
     */
    @Test
    void getBestWindow_aliObservatory_bortle1InReasons() {
        double lat = 32.5;
        double lng = 80.0;
        ForecastResult forecast = new ForecastResult(
                90, 5.0, "新月", 1, "极佳暗空", "今夜极佳!",
                1700000000L, 1700050000L);
        when(forecastService.forecast(lat, lng)).thenReturn(forecast);

        BestWindowResult result = bestWindowService.getBestWindow(lat, lng);

        assertThat(result.reasons()).contains("Bortle 1");
    }
}
