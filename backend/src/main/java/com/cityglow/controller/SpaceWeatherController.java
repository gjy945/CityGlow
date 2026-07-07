package com.cityglow.controller;

import com.cityglow.domain.AuroraForecastResult;
import com.cityglow.domain.DonkiResponse;
import com.cityglow.service.NasaDonkiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 太空天气(极光预报)REST 接口。
 *
 * <p>暴露 {@code GET /api/v1/space-weather/aurora?days=30},
 * 返回近 days 天的地磁暴与近 7 天的太阳耀斑事件列表(扁平化为统一事件列表),
 * 供前端判断是否有可观测极光的机会。</p>
 *
 * <p>该端点不需要认证(SecurityConfig 放行 {@code /api/v1/space-weather/**})。</p>
 *
 * <p>响应结构({@link AuroraForecastResult},对齐前端契约):</p>
 * <pre>{@code
 * {
 *   "totalCount": 3,
 *   "events": [
 *     {"id":"...", "type":"GST", "startTime":"...", "endTime":"...", "link":"...", "note":null},
 *     {"id":"...", "type":"FLR", "startTime":"...", "endTime":null, "link":"...", "note":"X1.5"}
 *   ]
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/v1/space-weather")
public class SpaceWeatherController {

    /** 默认查询近 30 天的地磁暴。 */
    private static final int DEFAULT_DAYS = 30;

    private final NasaDonkiClient donkiClient;

    public SpaceWeatherController(NasaDonkiClient donkiClient) {
        this.donkiClient = donkiClient;
    }

    /**
     * 查询极光预报(地磁暴 + 太阳耀斑,扁平化为统一事件列表)。
     *
     * @param days 地磁暴查询天数(可选,默认 30)
     * @return AuroraForecastResult,含 totalCount 与按时间倒序的 events 列表
     */
    @GetMapping("/aurora")
    public ResponseEntity<AuroraForecastResult> getAurora(
            @RequestParam(name = "days", defaultValue = "30") int days) {
        int safeDays = (days <= 0) ? DEFAULT_DAYS : days;
        DonkiResponse response = donkiClient.getAuroraForecast(safeDays);
        // 将后端 DonkiResponse(gstEvents + flrEvents 两个列表)转换为
        // 前端契约(扁平化 events 列表,按时间倒序)
        return ResponseEntity.ok(AuroraForecastResult.from(response));
    }
}
