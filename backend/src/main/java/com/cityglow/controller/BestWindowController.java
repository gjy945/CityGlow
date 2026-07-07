package com.cityglow.controller;

import com.cityglow.domain.BestWindowResult;
import com.cityglow.service.BestWindowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 今晚最佳观测时段推荐 REST 接口。
 *
 * <p>暴露 {@code GET /api/v1/best-window?lat=&lng=},
 * 返回今晚最适合观星的时间窗口、评分与原因列表。</p>
 *
 * <p>该端点不需要认证(SecurityConfig 放行 {@code /api/v1/best-window/**})。</p>
 *
 * <p>响应结构(BestWindowResult):</p>
 * <pre>{@code
 * {
 *   "date": "2026-07-07",
 *   "startTime": "21:15",
 *   "endTime": "03:23",
 *   "score": 78,
 *   "message": "今夜观测条件良好",
 *   "reasons": ["月落于 23:15", "云量仅 15%", "Bortle 3", "月相接近新月"]
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/v1/best-window")
public class BestWindowController {

    private final BestWindowService bestWindowService;

    public BestWindowController(BestWindowService bestWindowService) {
        this.bestWindowService = bestWindowService;
    }

    /**
     * 查询今晚的最佳观测时段。
     *
     * @param lat 纬度(必填,如 39.9)
     * @param lng 经度(必填,如 116.4)
     * @return BestWindowResult
     */
    @GetMapping
    public ResponseEntity<BestWindowResult> getBestWindow(
            @RequestParam double lat,
            @RequestParam double lng) {
        BestWindowResult result = bestWindowService.getBestWindow(lat, lng);
        return ResponseEntity.ok(result);
    }
}
