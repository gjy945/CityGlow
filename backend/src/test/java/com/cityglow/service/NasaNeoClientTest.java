package com.cityglow.service;

import com.cityglow.domain.NeoResponse;
import com.cityglow.domain.NeoResponse.Asteroid;
import com.cityglow.domain.NeoResponse.CloseApproachData;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * NasaNeoClient 单元测试。
 *
 * <p>用 Spring 的 {@link MockRestServiceServer} mock NASA NeoWs /feed 端点,
 * 不真实调用 NASA。验证:</p>
 * <ul>
 *   <li>/feed 端点 URL 含 start_date、end_date、api_key 参数。</li>
 *   <li>嵌套 estimated_diameter.kilometers.estimated_diameter_min/max 正确解析并转换为米。</li>
 *   <li>嵌套 relative_velocity.kilometers_per_hour 与 miss_distance.kilometers 正确拍平。</li>
 *   <li>NASA 自定义日期 "2026-Jul-07 00:00" 正确解析为 LocalDate。</li>
 *   <li>Map&lt;LocalDate, List&lt;Asteroid&gt;&gt; 结构正确反序列化。</li>
 *   <li>缓存命中:第二次调用 getFeed 不再发 HTTP 请求。</li>
 *   <li>days 参数 clamp 到 [1, 7] 范围。</li>
 * </ul>
 */
class NasaNeoClientTest {

    private static final String BASE_URL = "https://api.nasa.gov/neo/rest/v1";

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private NasaNeoClient client;
    private Cache<String, NeoResponse> neoCache;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        neoCache = Caffeine.newBuilder().build();
        client = new NasaNeoClient(restClientBuilder, "test-nasa-key", neoCache);
    }

    /**
     * 正常调用:返回 1 个日期 1 颗小行星,验证字段映射与单位转换。
     *
     * <p>NASA estimated_diameter 单位 km,record 字段 estimatedDiameterMinMeters/MaxMeters
     * 应为 km × 1000。如 min=0.5 km → 500.0 m,max=1.2 km → 1200.0 m。</p>
     *
     * <p>NASA 字符串数字 "55800" 应正确反序列化为 double 55800.0。</p>
     */
    @Test
    void getFeed_returnsAsteroidWithFlattenedFields_andUnitConversion() {
        String json = "{"
                + "\"near_earth_objects\":{"
                + "\"2026-07-07\":[{"
                + "\"id\":\"12345\","
                + "\"name\":\"433 Eros (A898 PA)\","
                + "\"absolute_magnitude_h\":11.16,"
                + "\"estimated_diameter\":{"
                + "\"kilometers\":{\"estimated_diameter_min\":0.5,\"estimated_diameter_max\":1.2}"
                + "},"
                + "\"is_potentially_hazardous_asteroid\":false,"
                + "\"close_approach_data\":[{"
                + "\"close_approach_date_full\":\"2026-Jul-07 00:00\","
                + "\"relative_velocity\":{\"kilometers_per_hour\":\"55800\"},"
                + "\"miss_distance\":{\"kilometers\":\"7500000\"}"
                + "}]"
                + "}]"
                + "}"
                + "}";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/feed")))
                .andExpect(queryParam("api_key", "test-nasa-key"))
                .andExpect(queryParam("start_date", "2026-07-07"))
                .andExpect(queryParam("end_date", "2026-07-14"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        LocalDate start = LocalDate.of(2026, 7, 7);
        LocalDate end = LocalDate.of(2026, 7, 14);
        NeoResponse response = client.getFeed(start, end);

        assertThat(response).isNotNull();
        Map<LocalDate, List<Asteroid>> neoMap = response.nearEarthObjects();
        assertThat(neoMap).containsKey(LocalDate.of(2026, 7, 7));

        List<Asteroid> asteroids = neoMap.get(LocalDate.of(2026, 7, 7));
        assertThat(asteroids).hasSize(1);

        Asteroid a = asteroids.get(0);
        assertThat(a.id()).isEqualTo("12345");
        assertThat(a.name()).isEqualTo("433 Eros (A898 PA)");
        assertThat(a.absoluteMagnitudeH()).isEqualTo(11.16);
        // 关键:km → m 转换(×1000)
        assertThat(a.estimatedDiameterMinMeters()).isEqualTo(500.0);
        assertThat(a.estimatedDiameterMaxMeters()).isEqualTo(1200.0);
        assertThat(a.isPotentiallyHazardous()).isFalse();

        // close_approach_data 拍平验证
        List<CloseApproachData> cad = a.closeApproachData();
        assertThat(cad).hasSize(1);
        // NASA 自定义日期 "2026-Jul-07 00:00" 解析为 LocalDate
        assertThat(cad.get(0).closeApproachDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        // NASA 字符串数字 "55800" → double 55800.0
        assertThat(cad.get(0).relativeVelocityKph()).isEqualTo(55800.0);
        assertThat(cad.get(0).missDistanceKm()).isEqualTo(7500000.0);
        mockServer.verify();
    }

    /**
     * 潜在危险小行星(is_potentially_hazardous_asteroid=true)与多个接近日期。
     */
    @Test
    void getFeed_potentiallyHazardousAsteroidWithMultipleApproaches() {
        String json = "{"
                + "\"near_earth_objects\":{"
                + "\"2026-07-08\":[{"
                + "\"id\":\"99999\","
                + "\"name\":\"(2026 XX)\","
                + "\"absolute_magnitude_h\":18.5,"
                + "\"estimated_diameter\":{"
                + "\"kilometers\":{\"estimated_diameter_min\":1.0,\"estimated_diameter_max\":2.5}"
                + "},"
                + "\"is_potentially_hazardous_asteroid\":true,"
                + "\"close_approach_data\":[{"
                + "\"close_approach_date_full\":\"2026-Jul-08 12:30\","
                + "\"relative_velocity\":{\"kilometers_per_hour\":\"100000\"},"
                + "\"miss_distance\":{\"kilometers\":\"5000000\"}"
                + "},{"
                + "\"close_approach_date_full\":\"2026-Jul-09 18:00\","
                + "\"relative_velocity\":{\"kilometers_per_hour\":\"95000\"},"
                + "\"miss_distance\":{\"kilometers\":\"6000000\"}"
                + "}]"
                + "}]"
                + "}"
                + "}";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/feed")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        LocalDate start = LocalDate.of(2026, 7, 7);
        LocalDate end = LocalDate.of(2026, 7, 14);
        NeoResponse response = client.getFeed(start, end);

        Asteroid a = response.nearEarthObjects().get(LocalDate.of(2026, 7, 8)).get(0);
        assertThat(a.isPotentiallyHazardous()).isTrue();
        assertThat(a.closeApproachData()).hasSize(2);
        assertThat(a.closeApproachData().get(0).closeApproachDate())
                .isEqualTo(LocalDate.of(2026, 7, 8));
        assertThat(a.closeApproachData().get(1).closeApproachDate())
                .isEqualTo(LocalDate.of(2026, 7, 9));
        mockServer.verify();
    }

    /**
     * NASA 偶发返回空 near_earth_objects:验证兜底为空 Map,无 NPE。
     */
    @Test
    void getFeed_emptyNearEarthObjects_returnsEmptyMap() {
        String json = "{\"near_earth_objects\":{}}";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/feed")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        LocalDate start = LocalDate.of(2026, 7, 7);
        LocalDate end = LocalDate.of(2026, 7, 14);
        NeoResponse response = client.getFeed(start, end);

        assertThat(response).isNotNull();
        assertThat(response.nearEarthObjects()).isEmpty();
        mockServer.verify();
    }

    /**
     * days 参数 clamp:传入 30 应被限制到 7(NASA /feed 上限)。
     *
     * <p>验证 getFeed(int days)的 endDate = today + min(days, 7)。</p>
     */
    @Test
    void getFeed_daysExceedingMax_clampedToSeven() {
        String json = "{\"near_earth_objects\":{}}";
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/feed")))
                .andExpect(queryParam("start_date", LocalDate.now().toString()))
                .andExpect(queryParam("end_date", LocalDate.now().plusDays(7).toString()))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // 传入 30 应被 clamp 到 7
        NeoResponse response = client.getFeed(30);

        assertThat(response).isNotNull();
        mockServer.verify();
    }

    /**
     * 缓存验证:第二次调用 getFeed(days) 命中缓存,不再发 HTTP 请求。
     */
    @Test
    void getFeed_secondCall_hitsCacheWithoutHttpCall() {
        String json = "{\"near_earth_objects\":{}}";
        // 只 expect 1 次请求(第一次调用)
        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/feed")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        // 第一次调用 → cache miss → 发请求
        NeoResponse first = client.getFeed(7);
        // 第二次调用 → cache hit → 不发请求
        NeoResponse second = client.getFeed(7);

        assertThat(first).isNotNull();
        // 缓存命中返回同一对象引用
        assertThat(second).isSameAs(first);
        // mockServer.verify() 校验只发了 1 次请求
        mockServer.verify();
    }
}
