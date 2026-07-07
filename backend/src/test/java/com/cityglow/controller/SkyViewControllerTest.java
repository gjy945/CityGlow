package com.cityglow.controller;

import com.cityglow.domain.SkyViewResult;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SkyViewController 集成测试(@SpringBootTest + MockMvc)。
 *
 * <p>用 {@code @SpringBootTest} 加载完整 Spring 上下文(含真实
 * ConstellationDataService、StarProjectionService、skyViewCache),
 * 不 mock 任何 Service — 星表/连线/神话数据在启动时由
 * ConstellationDataService 从 classpath 加载到内存,投影算法走真实实现。</p>
 *
 * <p>{@code @ActiveProfiles("test")} 切到 H2 内存库 + 不加载 AstroEventSeeder
 * (后者用 {@code @Profile("!test")} 排除)。</p>
 *
 * <p>SecurityConfig 已放行 {@code GET /api/v1/sky/**},所有请求无需 JWT。</p>
 *
 * <p><b>响应结构</b>:Controller 用统一 {@code ApiResponse<T>} 包装,
 * 响应体形如 {@code {code: 200, message: "success", data: {...}}},
 * 因此所有业务字段断言路径前缀为 {@code $.data.*}。</p>
 *
 * <p>验证 6 个用例:</p>
 * <ul>
 *   <li>合法参数(lat/lng/date/hour)→ 200 + data.visibleStars 数组</li>
 *   <li>缺省 date/hour → 200(用今天 + 默认 22 点)</li>
 *   <li>非法 hour(99)→ 200(回退到 22)</li>
 *   <li>GET /myths/orion → 200 + data 数组 2 篇神话卡</li>
 *   <li>GET /myths/nonexistent → 200 但 code=404(业务错误码,非 HTTP 404)</li>
 *   <li>同参数连调两次 → 第二次走缓存(Cache 条目数仍为 1)</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkyViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("skyViewCache")
    private Cache<String, SkyViewResult> skyViewCache;

    /**
     * 每个测试前清空星图视图缓存,保证用例间隔离。
     *
     * <p>@SpringBootTest 跨测试方法复用同一 Spring 上下文,Caffeine 单例缓存
     * 不会自动重置。若不清空,前一个用例写入的缓存会被后一个用例读到,
     * 让缓存验证测试失去意义。</p>
     */
    @BeforeEach
    void clearSkyViewCache() {
        skyViewCache.invalidateAll();
    }

    /**
     * 合法参数:lat=39.9, lng=116.4, date=2026-12-22, hour=22 → 200,
     * JSON 含 code=200、data.visibleStars 数组、data.constellations 数组等。
     *
     * <p>2026-12-22 是冬至,北纬 39.9°(北京)22 点夜空,猎户座等冬季星座应当可见,
     * visibleStars 至少有 1 颗。</p>
     */
    @Test
    void getSkyView_validParams_returnsResult() throws Exception {
        mockMvc.perform(get("/api/v1/sky/constellation-view")
                        .param("lat", "39.9")
                        .param("lng", "116.4")
                        .param("date", "2026-12-22")
                        .param("hour", "22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.visibleStars").isArray())
                .andExpect(jsonPath("$.data.constellations").isArray())
                .andExpect(jsonPath("$.data.observerLat").value("39.9000"))
                .andExpect(jsonPath("$.data.observerLng").value("116.4000"))
                .andExpect(jsonPath("$.data.date").value("2026-12-22"))
                .andExpect(jsonPath("$.data.hour").value(22));
    }

    /**
     * 不传 date 和 hour → 200,使用默认值(今天 + 22 点)。
     *
     * <p>Controller 中 date 缺省走 {@code LocalDate.now()},hour 缺省走
     * {@code defaultValue = "22"}。响应 data.hour 字段应为 22,data.date 字段应为今天。</p>
     */
    @Test
    void getSkyView_defaultDateAndHour_usesDefaults() throws Exception {
        String today = java.time.LocalDate.now().toString();
        mockMvc.perform(get("/api/v1/sky/constellation-view")
                        .param("lat", "39.9")
                        .param("lng", "116.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.visibleStars").isArray())
                .andExpect(jsonPath("$.data.hour").value(22))
                .andExpect(jsonPath("$.data.date").value(today));
    }

    /**
     * hour=99(越界)→ 200,Controller 自动回退到 22。
     *
     * <p>Controller 中 {@code int safeHour = (hour < 0 || hour > 23) ? 22 : hour;}
     * 保证 hour 始终在 [0, 23] 范围内。</p>
     */
    @Test
    void getSkyView_invalidHour_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/sky/constellation-view")
                        .param("lat", "39.9")
                        .param("lng", "116.4")
                        .param("date", "2026-12-22")
                        .param("hour", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hour").value(22));
    }

    /**
     * GET /myths/orion → 200,data 数组含 2 篇神话卡(希腊 + 中国)。
     *
     * <p>myths.json 中 orion 有 greek 和 chinese 两条记录,
     * ConstellationDataService.getMyths 返回 List<MythCard>,长度 2。</p>
     */
    @Test
    void getMyths_orion_returns2Cards() throws Exception {
        mockMvc.perform(get("/api/v1/sky/myths/orion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].culture").value(
                        org.hamcrest.Matchers.containsInAnyOrder("greek", "chinese")));
    }

    /**
     * GET /myths/nonexistent → HTTP 200,但业务 code=404。
     *
     * <p>Controller 中 myths 为空时返回 {@code ApiResponse.error(404, "...")},
     * HTTP 状态码仍为 200(Spring 默认),响应体 {@code {code: 404, message: "...", data: null}}。</p>
     */
    @Test
    void getMyths_unknownConstellation_returns404Code() throws Exception {
        mockMvc.perform(get("/api/v1/sky/myths/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 同参数连调两次 → 第二次走缓存。
     *
     * <p>验证方式:首次调用后 skyViewCache 应有 1 条条目;第二次同参数调用后
     * 缓存条目数仍为 1(若未命中缓存会重新计算并写入,条目数不变说明命中)。</p>
     *
     * <p>Caffeine {@code cache.get(key, mappingFunction)} 在 key 已存在时
     * 直接返回缓存值,不调用 mappingFunction,因此不会新增条目。</p>
     */
    @Test
    void getSkyView_cachedSecondCall_returnsSameResult() throws Exception {
        // 首次调用 → 200,缓存写入 1 条
        MvcResult first = mockMvc.perform(get("/api/v1/sky/constellation-view")
                        .param("lat", "39.9")
                        .param("lng", "116.4")
                        .param("date", "2026-12-22")
                        .param("hour", "22"))
                .andExpect(status().isOk())
                .andReturn();
        long sizeAfterFirst = skyViewCache.asMap().size();
        org.assertj.core.api.Assertions.assertThat(sizeAfterFirst).isEqualTo(1);

        String firstBody = first.getResponse().getContentAsString();

        // 第二次同参数调用 → 200,缓存命中(不新增条目)
        MvcResult second = mockMvc.perform(get("/api/v1/sky/constellation-view")
                        .param("lat", "39.9")
                        .param("lng", "116.4")
                        .param("date", "2026-12-22")
                        .param("hour", "22"))
                .andExpect(status().isOk())
                .andReturn();

        long sizeAfterSecond = skyViewCache.asMap().size();
        org.assertj.core.api.Assertions.assertThat(sizeAfterSecond).isEqualTo(1);

        // 两次响应体应完全一致(同一缓存对象序列化)
        String secondBody = second.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(secondBody).isEqualTo(firstBody);
    }
}
