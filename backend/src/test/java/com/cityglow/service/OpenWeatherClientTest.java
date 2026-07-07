package com.cityglow.service;

import com.cityglow.domain.OpenWeatherCurrentResponse;
import com.cityglow.domain.OpenWeatherOneCallResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * OpenWeatherClient 单元测试。
 *
 * <p>用 Spring 的 {@link MockRestServiceServer} mock HTTP 响应,
 * 不真实调用 OpenWeatherMap API。RestClient + Jackson 自动反序列化到 Record。</p>
 *
 * <p>Jackson 默认不会把 snake_case 的 {@code moon_phase} 映射到 record 组件
 * {@code moonPhase},因此 record 上用 {@code @JsonProperty("moon_phase")} 显式映射。</p>
 */
class OpenWeatherClientTest {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private OpenWeatherClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        // apiKey 用测试占位符(测试中不真实发请求,只校验请求 URL 含 appid)
        client = new OpenWeatherClient(restClientBuilder, "test-api-key");
    }

    // ---- /weather 端点 ----

    @Test
    void getCurrentWeather_returnsCloudCover() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/weather")))
                .andExpect(queryParam("lat", "39.9"))
                .andExpect(queryParam("lon", "116.4"))
                .andExpect(queryParam("appid", "test-api-key"))
                .andExpect(queryParam("units", "metric"))
                .andExpect(queryParam("lang", "zh_cn"))
                .andRespond(withSuccess(
                        "{\"clouds\":{\"all\":75},\"main\":{\"temp\":15.2,\"humidity\":60},"
                                + "\"weather\":[{\"description\":\"晴\"}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        OpenWeatherCurrentResponse resp = client.getCurrentWeather(39.9, 116.4);

        assertThat(resp).isNotNull();
        assertThat(resp.getCloudCover()).isEqualTo(75);
        assertThat(resp.main().temp()).isEqualTo(15.2);
        assertThat(resp.main().humidity()).isEqualTo(60);
        assertThat(resp.weather()).hasSize(1);
        assertThat(resp.weather().get(0).description()).isEqualTo("晴");
        mockServer.verify();
    }

    @Test
    void getCurrentWeather_nullClouds_returnsZeroCloudCover() {
        // OpenWeather 偶尔会缺字段,验证 getCloudCover 兜底
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/weather")))
                .andRespond(withSuccess("{}", org.springframework.http.MediaType.APPLICATION_JSON));

        OpenWeatherCurrentResponse resp = client.getCurrentWeather(0, 0);

        assertThat(resp.getCloudCover()).isEqualTo(0);
    }

    // ---- /onecall 端点 ----

    @Test
    void getOneCall_returnsMoonPhaseAndSunrise() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/onecall")))
                .andExpect(queryParam("lat", "39.9"))
                .andExpect(queryParam("lon", "116.4"))
                .andExpect(queryParam("appid", "test-api-key"))
                .andExpect(queryParam("exclude", "minutely,hourly,daily,alerts"))
                .andRespond(withSuccess(
                        "{\"current\":{\"sunrise\":1700000000,\"sunset\":1700050000,\"moon_phase\":0.5}}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        OpenWeatherOneCallResponse resp = client.getOneCall(39.9, 116.4);

        assertThat(resp).isNotNull();
        assertThat(resp.current()).isNotNull();
        assertThat(resp.current().sunrise()).isEqualTo(1700000000L);
        assertThat(resp.current().sunset()).isEqualTo(1700050000L);
        assertThat(resp.current().moonPhase()).isEqualTo(0.5);
        mockServer.verify();
    }

    // ---- moon_phase → illuminatedFraction 转换 ----

    @Test
    void moonPhaseConversion_newMoon() {
        // moon_phase=0 → (1-cos(0))/2 = 0
        assertThat(OpenWeatherClient.toMoonIlluminatedFraction(0.0))
                .isCloseTo(0.0, within(1e-9));
    }

    @Test
    void moonPhaseConversion_fullMoon() {
        // moon_phase=0.5 → (1-cos(π))/2 = (1-(-1))/2 = 1
        assertThat(OpenWeatherClient.toMoonIlluminatedFraction(0.5))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void moonPhaseConversion_firstQuarter() {
        // moon_phase=0.25 → (1-cos(π/2))/2 = (1-0)/2 = 0.5
        assertThat(OpenWeatherClient.toMoonIlluminatedFraction(0.25))
                .isCloseTo(0.5, within(1e-9));
    }

    @Test
    void moonPhaseConversion_lastQuarter() {
        // moon_phase=0.75 → (1-cos(3π/2))/2 = (1-0)/2 = 0.5
        assertThat(OpenWeatherClient.toMoonIlluminatedFraction(0.75))
                .isCloseTo(0.5, within(1e-9));
    }

    @Test
    void moonPhaseConversion_newMoonAtOne() {
        // moon_phase=1 → 等价新月,(1-cos(2π))/2 = 0
        assertThat(OpenWeatherClient.toMoonIlluminatedFraction(1.0))
                .isCloseTo(0.0, within(1e-9));
    }
}
