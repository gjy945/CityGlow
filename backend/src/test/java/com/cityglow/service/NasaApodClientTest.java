package com.cityglow.service;

import com.cityglow.domain.NasaApodResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * NasaApodClient 单元测试。
 *
 * <p>用 Spring 的 {@link MockRestServiceServer} mock NASA API 响应,
 * 不真实调用 NASA 端点。验证:</p>
 * <ul>
 *   <li>RestClient + Jackson 正确反序列化到 {@link NasaApodResponse} record。</li>
 *   <li>NASA snake_case 字段 {@code media_type} 通过
 *       {@code @JsonProperty("media_type")} 正确映射到 record 组件 {@code mediaType}。</li>
 *   <li>请求 URL 含 {@code api_key} 查询参数。</li>
 *   <li>{@code date} 参数构造的请求 URL 含 {@code date} 查询参数。</li>
 * </ul>
 */
class NasaApodClientTest {

    private static final String BASE_URL = "https://api.nasa.gov/planetary/apod";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private NasaApodClient client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        // apiKey 用测试占位符(测试中不真实发请求,只校验请求 URL 含 api_key)
        client = new NasaApodClient(restClientBuilder, "test-nasa-key");
    }

    /**
     * getApod() → 解析今日 APOD,验证字段映射(尤其 media_type → mediaType)。
     */
    @Test
    void getApod_returnsParsedResponse_withMediaTypeMapping() {
        String json = "{"
                + "\"title\":\"Andromeda Island Universe\","
                + "\"explanation\":\"M31 是距离银河系最近的大型旋涡星系。\","
                + "\"url\":\"https://apod.nasa.gov/apod/image/andromeda.jpg\","
                + "\"hdurl\":\"https://apod.nasa.gov/apod/image/andromeda_hd.jpg\","
                + "\"media_type\":\"image\","
                + "\"date\":\"2026-07-07\","
                + "\"copyright\":\"NASA\""
                + "}";
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        NasaApodResponse resp = client.getApod();

        assertThat(resp).isNotNull();
        assertThat(resp.title()).isEqualTo("Andromeda Island Universe");
        assertThat(resp.explanation()).contains("M31");
        assertThat(resp.url()).contains("andromeda.jpg");
        assertThat(resp.hdurl()).contains("andromeda_hd.jpg");
        // 关键:NASA 返回 media_type,record 组件 mediaType 必须正确映射
        assertThat(resp.mediaType()).isEqualTo("image");
        assertThat(resp.date()).isEqualTo("2026-07-07");
        assertThat(resp.copyright()).isEqualTo("NASA");
        mockServer.verify();
    }

    /**
     * getApod() → NASA 偶尔缺 hdurl/copyright 字段,验证 null 容错。
     */
    @Test
    void getApod_missingOptionalFields_returnsNulls() {
        String json = "{"
                + "\"title\":\"Video APOD\","
                + "\"explanation\":\"某视频类型 APOD。\","
                + "\"url\":\"https://apod.nasa.gov/apod/image/video.mp4\","
                + "\"media_type\":\"video\","
                + "\"date\":\"2026-07-07\""
                + "}";
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        NasaApodResponse resp = client.getApod();

        assertThat(resp).isNotNull();
        assertThat(resp.hdurl()).isNull();
        assertThat(resp.copyright()).isNull();
        assertThat(resp.mediaType()).isEqualTo("video");
        mockServer.verify();
    }

    /**
     * getApod(date) → 请求 URL 含 date 查询参数,响应正确解析。
     */
    @Test
    void getApod_withDate_includesDateQueryParam() {
        String json = "{"
                + "\"title\":\"Historical APOD\","
                + "\"explanation\":\"历史 APOD。\","
                + "\"url\":\"https://apod.nasa.gov/apod/image/old.jpg\","
                + "\"media_type\":\"image\","
                + "\"date\":\"2026-01-01\""
                + "}";
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andExpect(queryParam("date", "2026-01-01"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        NasaApodResponse resp = client.getApod("2026-01-01");

        assertThat(resp).isNotNull();
        assertThat(resp.title()).isEqualTo("Historical APOD");
        assertThat(resp.date()).isEqualTo("2026-01-01");
        mockServer.verify();
    }
}
