package com.cityglow.service;

import com.cityglow.domain.OpenMeteoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * OpenMeteoClient 单元测试。
 *
 * <p>用 Spring 的 {@link MockRestServiceServer} mock HTTP 响应,
 * 不真实调用 Open-Meteo API。RestClient + Jackson 自动反序列化到 Record。</p>
 *
 * <p>验证:</p>
 * <ul>
 *   <li>/forecast 端点请求参数正确(latitude/longitude/current/daily/timezone)</li>
 *   <li>响应正确反序列化到 OpenMeteoResponse record</li>
 *   <li>ISO 8601 时间字符串转 unix 秒正确</li>
 *   <li>null/空 daily 兜底返回 null</li>
 * </ul>
 */
class OpenMeteoClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private OpenMeteoClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new OpenMeteoClient(restClientBuilder);
    }

    @Test
    void getForecast_returnsCloudCoverAndSunriseSunset() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/forecast")))
                .andExpect(queryParam("latitude", "39.9"))
                .andExpect(queryParam("longitude", "116.4"))
                .andExpect(queryParam("current", "cloud_cover,temperature_2m,relative_humidity_2m,weather_code"))
                .andExpect(queryParam("daily", "sunrise,sunset"))
                .andExpect(queryParam("timezone", "auto"))
                .andRespond(withSuccess(
                        "{\"current\":{\"cloud_cover\":63,\"temperature_2m\":25.2,"
                                + "\"relative_humidity_2m\":86,\"weather_code\":2},"
                                + "\"daily\":{\"sunrise\":[\"2026-07-07T04:53\"],"
                                + "\"sunset\":[\"2026-07-07T19:45\"]}}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        OpenMeteoResponse resp = client.getForecast(39.9, 116.4);

        assertThat(resp).isNotNull();
        assertThat(resp.current()).isNotNull();
        assertThat(resp.current().cloudCover()).isEqualTo(63);
        assertThat(resp.current().temperature2m()).isEqualTo(25.2);
        assertThat(resp.current().relativeHumidity2m()).isEqualTo(86);
        assertThat(resp.current().weatherCode()).isEqualTo(2);
        assertThat(resp.getTodaySunrise()).isEqualTo("2026-07-07T04:53");
        assertThat(resp.getTodaySunset()).isEqualTo("2026-07-07T19:45");
        mockServer.verify();
    }

    @Test
    void getForecast_nullDaily_returnsNullSunrise() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/forecast")))
                .andRespond(withSuccess(
                        "{\"current\":{\"cloud_cover\":0,\"temperature_2m\":10,"
                                + "\"relative_humidity_2m\":50,\"weather_code\":0}}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        OpenMeteoResponse resp = client.getForecast(0, 0);

        assertThat(resp).isNotNull();
        assertThat(resp.getTodaySunrise()).isNull();
        assertThat(resp.getTodaySunset()).isNull();
    }

    @Test
    void isoToEpochSecond_validIso_returnsEpochSeconds() {
        // "2026-07-07T04:53" → UTC epoch
        long epoch = OpenMeteoClient.isoToEpochSecond("2026-07-07T04:53");
        assertThat(epoch).isGreaterThan(0);
    }

    @Test
    void isoToEpochSecond_nullOrBlank_returnsZero() {
        assertThat(OpenMeteoClient.isoToEpochSecond(null)).isEqualTo(0);
        assertThat(OpenMeteoClient.isoToEpochSecond("")).isEqualTo(0);
        assertThat(OpenMeteoClient.isoToEpochSecond("  ")).isEqualTo(0);
    }

    @Test
    void isoToEpochSecond_invalidFormat_returnsZero() {
        assertThat(OpenMeteoClient.isoToEpochSecond("not-a-date")).isEqualTo(0);
    }

    @Test
    void getTodaySunrise_emptyList_returnsNull() {
        OpenMeteoResponse resp = new OpenMeteoResponse(
                new OpenMeteoResponse.Current(0, 0, 0, 0),
                new OpenMeteoResponse.Daily(List.of(), List.of()));
        assertThat(resp.getTodaySunrise()).isNull();
    }
}
