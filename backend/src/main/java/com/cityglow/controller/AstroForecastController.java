package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.domain.ForecastResult;
import com.cityglow.service.ForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
 *   "data": { "score": 85, "cloudCover": 10, "moonPhase": "New Moon",
 *             "bortleLevel": 3, "message": "今夜极佳!", "sunrise": ..., "sunset": ... }
 * }
 * }</pre>
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
     * 查询给定经纬度的观星预报。
     *
     * @param lat 纬度(必填,如 39.9)
     * @param lng 经度(必填,如 116.4)
     * @return 统一 ApiResponse 包装的 ForecastResult
     */
    @GetMapping("/forecast")
    public ApiResponse<ForecastResult> forecast(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ApiResponse.success(forecastService.forecast(lat, lng));
    }
}
