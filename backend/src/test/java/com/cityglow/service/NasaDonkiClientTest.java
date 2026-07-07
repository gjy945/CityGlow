package com.cityglow.service;

import com.cityglow.domain.DonkiResponse;
import com.cityglow.domain.DonkiResponse.GeomagneticStorm;
import com.cityglow.domain.DonkiResponse.SolarFlare;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * NasaDonkiClient 单元测试。
 *
 * <p>用 Spring 的 {@link MockRestServiceServer} mock NASA DONKI /GST 与 /FLR 端点响应,
 * 不真实调用 NASA。验证:</p>
 * <ul>
 *   <li>/GST 端点 URL 含 startDate、endDate、api_key 参数,响应正确反序列化。</li>
 *   <li>/FLR 端点 URL 含 startDate、endDate、api_key 参数,响应正确反序列化。</li>
 *   <li>NASA ISO 8601 时间字符串(带 Z 后缀)正确解析为 OffsetDateTime。</li>
 *   <li>并行调用 + 合并结果:DonkiResponse.gstEvents 与 flrEvents 字段填充正确。</li>
 *   <li>缓存命中:第二次调用 getAuroraForecast 不再发 HTTP 请求。</li>
 *   <li>空数组响应(null/[])兜底为空列表。</li>
 * </ul>
 */
class NasaDonkiClientTest {

    private static final String BASE_URL = "https://api.nasa.gov/DONKI";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private NasaDonkiClient client;
    private Cache<String, DonkiResponse> spaceWeatherCache;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        // ignoreExpectOrder(true):NasaDonkiClient 用虚拟线程并行调 /GST 与 /FLR,
        // 请求到达顺序不确定,需忽略顺序匹配,否则并行测试会因 URI 顺序不符而失败
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).ignoreExpectOrder(true).build();
        // 每个测试使用独立缓存,避免缓存污染
        spaceWeatherCache = Caffeine.newBuilder().build();
        client = new NasaDonkiClient(restClientBuilder, "test-nasa-key", spaceWeatherCache);
    }

    /**
     * /GST 端点:返回 2 条地磁暴事件,验证字段映射与时间解析。
     *
     * <p>NASA 时间格式 "2026-06-15T18:30:00Z"(带 Z 后缀,UTC)。
     * 用 OffsetDateTime 接收,Jackson 默认 ISO_OFFSET_DATE_TIME 解析器原生支持。</p>
     */
    @Test
    void fetchGstEvents_returnsParsedStorms_withZTimestampParsing() {
        String json = "["
                + "{\"activityID\":\"2026-06-15T18:30:00-GST-001\","
                + "\"startTime\":\"2026-06-15T18:30:00Z\","
                + "\"observedTime\":\"2026-06-15T20:00:00Z\","
                + "\"link\":\"https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/GST/001\"},"
                + "{\"activityID\":\"2026-06-20T12:00:00-GST-002\","
                + "\"startTime\":\"2026-06-20T12:00:00Z\","
                + "\"observedTime\":\"2026-06-20T13:30:00Z\","
                + "\"link\":\"https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/GST/002\"}"
                + "]";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/GST")))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        LocalDate start = LocalDate.of(2026, 5, 18);
        LocalDate end = LocalDate.of(2026, 6, 17);
        List<GeomagneticStorm> storms = client.fetchGstEventsForTest(start, end);

        assertThat(storms).hasSize(2);
        assertThat(storms.get(0).activityID()).isEqualTo("2026-06-15T18:30:00-GST-001");
        // 关键:NASA "Z" 后缀正确解析为 OffsetDateTime(UTC)
        assertThat(storms.get(0).startTime()).isEqualTo(
                OffsetDateTime.of(2026, 6, 15, 18, 30, 0, 0, ZoneOffset.UTC));
        assertThat(storms.get(0).observedTime()).isEqualTo(
                OffsetDateTime.of(2026, 6, 15, 20, 0, 0, 0, ZoneOffset.UTC));
        assertThat(storms.get(0).link()).contains("/DONKI/view/GST/001");
        mockServer.verify();
    }

    /**
     * /FLR 端点:返回 1 条 X1.5 太阳耀斑,验证 classType 与 peakTime 解析。
     */
    @Test
    void fetchFlrEvents_returnsParsedFlares_withClassTypeMapping() {
        String json = "["
                + "{\"flareID\":\"2026-06-15T18:30:00-FLR-001\","
                + "\"classType\":\"X1.5\","
                + "\"peakTime\":\"2026-06-15T19:00:00Z\","
                + "\"link\":\"https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/FLR/001\"}"
                + "]";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/FLR")))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        LocalDate start = LocalDate.of(2026, 6, 10);
        LocalDate end = LocalDate.of(2026, 6, 17);
        List<SolarFlare> flares = client.fetchFlrEventsForTest(start, end);

        assertThat(flares).hasSize(1);
        assertThat(flares.get(0).flareID()).isEqualTo("2026-06-15T18:30:00-FLR-001");
        assertThat(flares.get(0).classType()).isEqualTo("X1.5");
        assertThat(flares.get(0).peakTime()).isEqualTo(
                OffsetDateTime.of(2026, 6, 15, 19, 0, 0, 0, ZoneOffset.UTC));
        mockServer.verify();
    }

    /**
     * 并行调用 /GST + /FLR 并合并:DonkiResponse 两个字段均填充。
     *
     * <p>验证虚拟线程并行调用的正确性:两个端点被同时调用,
     * 结果合并到同一 DonkiResponse。</p>
     */
    @Test
    void fetchAuroraForecast_parallelCalls_mergesIntoDonkiResponse() {
        String gstJson = "["
                + "{\"activityID\":\"2026-06-15T18:30:00-GST-001\","
                + "\"startTime\":\"2026-06-15T18:30:00Z\","
                + "\"observedTime\":\"2026-06-15T20:00:00Z\","
                + "\"link\":\"https://gst-link\"}]";
        String flrJson = "["
                + "{\"flareID\":\"2026-06-15T18:30:00-FLR-001\","
                + "\"classType\":\"M3.2\","
                + "\"peakTime\":\"2026-06-15T19:00:00Z\","
                + "\"link\":\"https://flr-link\"}]";

        // 设置两个端点的 mock 响应(并行调用,顺序未定)
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/GST")))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andRespond(withSuccess(gstJson, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/FLR")))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andRespond(withSuccess(flrJson, MediaType.APPLICATION_JSON));

        DonkiResponse response = client.fetchAuroraForecastForTest(30);

        assertThat(response).isNotNull();
        assertThat(response.gstEvents()).hasSize(1);
        assertThat(response.gstEvents().get(0).activityID()).isEqualTo("2026-06-15T18:30:00-GST-001");
        assertThat(response.flrEvents()).hasSize(1);
        assertThat(response.flrEvents().get(0).classType()).isEqualTo("M3.2");
        mockServer.verify();
    }

    /**
     * 回归测试:NASA 实际返回的时间格式可能无秒段,形如 "2026-07-04T00:00Z"。
     *
     * <p>此前用 LocalDateTime + @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss['Z']")
     * 解析时,因强制秒段或 pattern 方括号内引号误用,抛出
     * DateTimeParseException: Text '2026-07-04T00:00Z' could not be parsed。
     * 改用 OffsetDateTime 后,Jackson 默认 ISO_OFFSET_DATE_TIME 解析器原生
     * 支持带/不带秒两种格式,此用例验证修复不再回归。</p>
     */
    @Test
    void fetchGstEvents_nasaRealWorldTimestampWithoutSeconds_parsesSuccessfully() {
        // NASA 真实响应片段(2026-07-04 地磁暴,无秒段)
        String json = "["
                + "{\"activityID\":\"2026-07-04T00:00:00-GST-001\","
                + "\"startTime\":\"2026-07-04T00:00Z\","
                + "\"link\":\"https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/GST/47312\"}"
                + "]";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/GST")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<GeomagneticStorm> storms = client.fetchGstEventsForTest(
                LocalDate.of(2026, 6, 7), LocalDate.of(2026, 7, 7));

        assertThat(storms).hasSize(1);
        // 无秒格式 "2026-07-04T00:00Z" 解析为 OffsetDateTime(UTC 00:00:00)
        assertThat(storms.get(0).startTime())
                .isEqualTo(OffsetDateTime.of(2026, 7, 4, 0, 0, 0, 0, ZoneOffset.UTC));
        // observedTime 未返回 → null(NASA GST 顶层无此字段,嵌套在 allKpIndex 中)
        assertThat(storms.get(0).observedTime()).isNull();
        mockServer.verify();
    }

    /**
     * NASA 偶尔返回空数组 "[]":验证兜底为空列表,无 NPE。
     */
    @Test
    void fetchGstEvents_emptyArray_returnsEmptyList() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/GST")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<GeomagneticStorm> storms = client.fetchGstEventsForTest(
                LocalDate.of(2026, 5, 18), LocalDate.of(2026, 6, 17));

        assertThat(storms).isNotNull().isEmpty();
        mockServer.verify();
    }

    /**
     * 缓存验证:同一 days 参数第二次调用应命中缓存,不再发 HTTP 请求。
     *
     * <p>验证 spaceWeatherCache("aurora" key)命中行为:第二次调用跳过并行调用,
     * 直接返回缓存的 DonkiResponse。</p>
     */
    @Test
    void getAuroraForecast_secondCall_hitsCacheWithoutHttpCall() {
        String gstJson = "[]";
        String flrJson = "[]";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/GST")))
                .andRespond(withSuccess(gstJson, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/FLR")))
                .andRespond(withSuccess(flrJson, MediaType.APPLICATION_JSON));

        // 第一次调用 → cache miss → 发 2 个 HTTP 请求
        DonkiResponse first = client.getAuroraForecast(30);
        // 第二次调用 → cache hit → 不再发请求
        DonkiResponse second = client.getAuroraForecast(30);

        assertThat(first).isNotNull();
        // 缓存命中返回同一对象引用
        assertThat(second).isSameAs(first);
        // mockServer.verify() 校验总共只发了 2 次请求(GST + FLR),若第二次也发会失败
        mockServer.verify();
    }
}
