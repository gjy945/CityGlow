package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.domain.ForecastResult;
import com.cityglow.service.ForecastService;
import com.cityglow.util.Messages;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * 观星预报 REST 接口(设计文档第 4 节模块 2、第 5.1 节接口契约)。
 *
 * <p>暴露 {@code GET /api/v1/astro/forecast?lat=&lng=},委托给
 * {@link ForecastService} 计算观星指数,用统一 {@link ApiResponse} 包装返回。</p>
 *
 * <p>响应结构:</p>
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { "score": 85, "cloudCover": 10, "moonPhase": "新月",
 *             "bortleLevel": 3, "bortleDescription": "暗空良好",
 *             "message": "今夜极佳!", "sunrise": ..., "sunset": ... }
 * }
 * }</pre>
 *
 * <p><b>多语言</b>:从 Accept-Language header 解析 Locale(zh/en/ja,默认 zh),
 * 传给 ForecastService 决定月相/Bortle/消息描述语言。</p>
 *
 * <p>缺 lat 或 lng 参数时 Spring MVC 自动返回 400 Bad Request。</p>
 */
@RestController
@RequestMapping("/api/v1/astro")
public class AstroForecastController {

    private final ForecastService forecastService;

    public AstroForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    /**
     * 查询给定经纬度的观星预报,按 Accept-Language 返回多语言描述。
     *
     * @param lat     纬度(必填,如 39.9)
     * @param lng     经度(必填,如 116.4)
     * @param request HTTP 请求(用于解析 Accept-Language)
     * @return 统一 ApiResponse 包装的 ForecastResult
     */
    @GetMapping("/forecast")
    public ApiResponse<ForecastResult> forecast(
            @RequestParam double lat,
            @RequestParam double lng,
            HttpServletRequest request) {
        Locale locale = Messages.resolveLocale(request);
        return ApiResponse.success(forecastService.forecast(lat, lng, locale));
    }
}
