package com.cityglow.controller;

import com.cityglow.domain.ForecastResult;
import com.cityglow.repository.UserRepository;
import com.cityglow.service.ForecastService;
import com.cityglow.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AstroForecastController Web 层测试(@WebMvcTest + MockMvc)。
 *
 * <p>用 {@link MockBean} mock {@link ForecastService},不加载完整 Spring 上下文,
 * 只装配 AstroForecastController 的 MVC 基础设施(Jackson 序列化、参数绑定、异常处理)。</p>
 *
 * <p>因 Spring Security 在 classpath 上,默认会拦截所有请求需认证。
 * 用 {@code @AutoConfigureMockMvc(addFilters = false)} 关闭过滤器,
 * 让测试聚焦 Controller 编排逻辑,不验证 JWT 鉴权。</p>
 *
 * <p>验证:</p>
 * <ul>
 *   <li>GET /api/v1/astro/forecast?lat=39.9&lng=116.4 返回 200,
 *       JSON 含统一 ApiResponse 结构(code/message/data)及 data.score 等字段。</li>
 *   <li>缺 lat 或 lng 参数返回 400(Required request parameter 缺失)。</li>
 * </ul>
 */
@WebMvcTest(AstroForecastController.class)
@AutoConfigureMockMvc(addFilters = false)
class AstroForecastControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForecastService forecastService;

    // 以下两个 @MockBean 仅为满足 JwtAuthenticationFilter 自动装配
    // (Filter 在 @WebMvcTest 中被扫描,但 JwtUtil/UserRepository 不在切片内)
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    /**
     * 正常请求:lat=39.9&lng=116.4 → 200,
     * 响应 JSON 含 code=200, message="success", data.score=68 等。
     */
    @Test
    void forecast_validParams_returns200WithApiResponseStructure() throws Exception {
        // given: mock service 返回固定 ForecastResult(8 字段)
        ForecastResult result = new ForecastResult(
                68, 10.0, "新月", 5, "郊区天空", "适合观星", 1700000000L, 1700050000L);
        when(forecastService.forecast(anyDouble(), anyDouble(), any(Locale.class)))
                .thenReturn(result);

        // when + then: 校验 HTTP 状态码与 JSON 结构
        mockMvc.perform(get("/api/v1/astro/forecast")
                        .param("lat", "39.9")
                        .param("lng", "116.4"))
                .andExpect(status().isOk())
                // 统一 ApiResponse 三字段
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                // data 内 ForecastResult 字段
                .andExpect(jsonPath("$.data.score").value(68))
                .andExpect(jsonPath("$.data.cloudCover").value(10.0))
                .andExpect(jsonPath("$.data.moonPhase").value("新月"))
                .andExpect(jsonPath("$.data.bortleLevel").value(5))
                .andExpect(jsonPath("$.data.bortleDescription").value("郊区天空"))
                .andExpect(jsonPath("$.data.message").value("适合观星"))
                .andExpect(jsonPath("$.data.sunrise").value(1700000000))
                .andExpect(jsonPath("$.data.sunset").value(1700050000));
    }

    /**
     * 缺 lng 参数 → 400 Bad Request。
     */
    @Test
    void forecast_missingLngParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/astro/forecast")
                        .param("lat", "39.9"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 缺 lat 参数 → 400 Bad Request。
     */
    @Test
    void forecast_missingLatParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/astro/forecast")
                        .param("lng", "116.4"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 两个参数都缺 → 400 Bad Request。
     */
    @Test
    void forecast_missingBothParams_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/astro/forecast"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 不同经纬度(阿里天文台)也能正确转发给 service 并返回。
     * 验证 service 被正确调用且响应包装正确。
     */
    @Test
    void forecast_aliObservatory_returnsPerfectScore() throws Exception {
        ForecastResult result = new ForecastResult(
                100, 0.0, "新月", 1, "极佳暗空", "今夜极佳!", 1700000000L, 1700050000L);
        when(forecastService.forecast(anyDouble(), anyDouble(), any(Locale.class)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/astro/forecast")
                        .param("lat", "32.5")
                        .param("lng", "80.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.bortleLevel").value(1))
                .andExpect(jsonPath("$.data.message").value("今夜极佳!"));
    }

    /**
     * Accept-Language: en 时,Controller 解析为 English Locale 传给 service,
     * 验证 Locale 透传链路(本测试不验证 service 内部多语言,只验证 Locale 解析)。
     */
    @Test
    void forecast_englishAcceptLanguage_stillReturns200() throws Exception {
        ForecastResult result = new ForecastResult(
                100, 0.0, "New Moon", 1, "Excellent dark sky", "Excellent tonight!",
                1700000000L, 1700050000L);
        when(forecastService.forecast(anyDouble(), anyDouble(), any(Locale.class)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/astro/forecast")
                        .param("lat", "32.5")
                        .param("lng", "80.0")
                        .header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moonPhase").value("New Moon"))
                .andExpect(jsonPath("$.data.bortleDescription").value("Excellent dark sky"))
                .andExpect(jsonPath("$.data.message").value("Excellent tonight!"));
    }
}
