package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenWeatherCurrentResponse;
import com.cityglow.domain.OpenWeatherOneCallResponse;
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
 * 验证:</p>
 * <ul>
 *   <li>正常调用:新月+云量 10+Bortle 5(北京) → score=68</li>
 *   <li>满月:moonPhase=0.5 → moonIlluminatedFraction=1.0,score 相应降低</li>
 *   <li>null 响应兜底:云量 100,moonPhase 0,score clamp 到 0</li>
 * </ul>
 *
 * <p>虚拟线程并行调用由 ForecastService 内部用
 * {@code Executors.newVirtualThreadPerTaskExecutor()} 实现,
 * 此处通过 mock 验证两个端点都被调用。</p>
 */
@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private OpenWeatherClient openWeatherClient;

    @InjectMocks
    private ForecastService forecastService;

    /**
     * 北京 (39.9, 116.4) → Bortle 5(默认)。
     * 新月 moonPhase=0 → moonIlluminatedFraction=0。
     * 云量 10(≤20 不扣分)。
     * score = 100 - 0 - 0 - (5-1)*8 = 68,message="适合观星"。
     */
    @Test
    void forecast_newMoon_lowCloudCover_beijing_returnsScore68() {
        // given: 北京坐标 → Bortle 5
        double lat = 39.9;
        double lng = 116.4;

        OpenWeatherCurrentResponse current =
                new OpenWeatherCurrentResponse(
                        new OpenWeatherCurrentResponse.Clouds(10),
                        null, null);
        OpenWeatherOneCallResponse oneCall =
                new OpenWeatherOneCallResponse(
                        new OpenWeatherOneCallResponse.Current(
                                1700000000L, 1700050000L, 0.0));

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(current);
        when(openWeatherClient.getOneCall(lat, lng)).thenReturn(oneCall);

        // when
        ForecastResult result = forecastService.forecast(lat, lng);

        // then
        assertThat(result.score()).isEqualTo(68);
        assertThat(result.cloudCover()).isEqualTo(10.0);
        assertThat(result.moonPhase()).isEqualTo("New Moon");
        assertThat(result.bortleLevel()).isEqualTo(5);
        assertThat(result.message()).isEqualTo("适合观星");
        assertThat(result.sunrise()).isEqualTo(1700000000L);
        assertThat(result.sunset()).isEqualTo(1700050000L);
    }

    /**
     * 满月 moonPhase=0.5 → moonIlluminatedFraction=1.0。
     * 云量 0,Bortle 5(北京)。
     * score = 100 - 0 - 1.0*30 - (5-1)*8 = 100 - 30 - 32 = 38,message="不建议"。
     */
    @Test
    void forecast_fullMoon_zeroCloudCover_scoreReducedBy30() {
        double lat = 39.9;
        double lng = 116.4;

        OpenWeatherCurrentResponse current =
                new OpenWeatherCurrentResponse(
                        new OpenWeatherCurrentResponse.Clouds(0),
                        null, null);
        OpenWeatherOneCallResponse oneCall =
                new OpenWeatherOneCallResponse(
                        new OpenWeatherOneCallResponse.Current(
                                1700000000L, 1700050000L, 0.5));

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(current);
        when(openWeatherClient.getOneCall(lat, lng)).thenReturn(oneCall);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.score()).isEqualTo(38);
        assertThat(result.moonPhase()).isEqualTo("Full Moon");
        assertThat(result.cloudCover()).isEqualTo(0.0);
        assertThat(result.message()).isEqualTo("不建议");
    }

    /**
     * 阿里天文台 (32.5, 80.0) → Bortle 1(极佳暗空)。
     * 新月 + 云量 0 + Bortle 1 → score = 100 - 0 - 0 - 0 = 100,message="今夜极佳!"。
     */
    @Test
    void forecast_aliObservatory_bortle1_perfectScore100() {
        double lat = 32.5;
        double lng = 80.0;

        OpenWeatherCurrentResponse current =
                new OpenWeatherCurrentResponse(
                        new OpenWeatherCurrentResponse.Clouds(0),
                        null, null);
        OpenWeatherOneCallResponse oneCall =
                new OpenWeatherOneCallResponse(
                        new OpenWeatherOneCallResponse.Current(
                                1700000000L, 1700050000L, 0.0));

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(current);
        when(openWeatherClient.getOneCall(lat, lng)).thenReturn(oneCall);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.bortleLevel()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.message()).isEqualTo("今夜极佳!");
    }

    /**
     * null 响应兜底:OpenWeatherClient 返回 null。
     * cloudCover 兜底 100,moonPhase 兜底 0,sunrise/sunset 兜底 0。
     * score = 100 - (100-20)*1.5 - 0 - 32 = 100 - 120 - 32 = -52 → clamp 0。
     */
    @Test
    void forecast_nullResponses_fallbacksApplied_scoreClampedTo0() {
        double lat = 39.9;
        double lng = 116.4;

        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(null);
        when(openWeatherClient.getOneCall(lat, lng)).thenReturn(null);

        ForecastResult result = forecastService.forecast(lat, lng);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.cloudCover()).isEqualTo(100.0);
        assertThat(result.moonPhase()).isEqualTo("New Moon");
        assertThat(result.bortleLevel()).isEqualTo(5);
        assertThat(result.message()).isEqualTo("不建议");
        assertThat(result.sunrise()).isEqualTo(0L);
        assertThat(result.sunset()).isEqualTo(0L);
    }

    /**
     * oneCall.current() 为 null 时,sunrise/sunset/moonPhase 均兜底。
     */
    @Test
    void forecast_oneCallCurrentNull_moonPhaseSunriseFallbackToZero() {
        double lat = 39.9;
        double lng = 116.4;

        OpenWeatherCurrentResponse current =
                new OpenWeatherCurrentResponse(
                        new OpenWeatherCurrentResponse.Clouds(0),
                        null, null);
        when(openWeatherClient.getCurrentWeather(lat, lng)).thenReturn(current);
        when(openWeatherClient.getOneCall(lat, lng))
                .thenReturn(new OpenWeatherOneCallResponse(null));

        ForecastResult result = forecastService.forecast(lat, lng);

        // Bortle 5 + 新月 + 云量 0 = 100 - 0 - 0 - 32 = 68
        assertThat(result.score()).isEqualTo(68);
        assertThat(result.sunrise()).isEqualTo(0L);
        assertThat(result.sunset()).isEqualTo(0L);
        assertThat(result.moonPhase()).isEqualTo("New Moon");
    }
}
