package com.cityglow.controller;

import com.cityglow.domain.NeoResponse;
import com.cityglow.service.NasaNeoClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 近地小行星(NeoWs)REST 接口。
 *
 * <p>暴露 {@code GET /api/v1/neo/feed?days=7},
 * 返回未来 days 天接近地球的小行星列表(按日期分组)。</p>
 *
 * <p>该端点不需要认证(SecurityConfig 放行 {@code /api/v1/neo/**})。</p>
 *
 * <p>NASA NeoWs /feed 日期范围上限 7 天,Controller 自动 clamp。</p>
 *
 * <p>响应结构(NeoResponse):</p>
 * <pre>{@code
 * {
 *   "near_earth_objects": {
 *     "2026-07-07": [{"id":"...", "name":"...", "absoluteMagnitudeH":18.5,
 *                      "estimatedDiameterMinMeters":50.0, "estimatedDiameterMaxMeters":120.0,
 *                      "isPotentiallyHazardous":false,
 *                      "closeApproachData":[{"closeApproachDate":"2026-07-07",
 *                                            "relativeVelocityKph":55800,
 *                                            "missDistanceKm":7500000}]}]
 *   }
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/v1/neo")
public class NeoController {

    /** 默认查未来 7 天。 */
    private static final int DEFAULT_DAYS = 7;

    private final NasaNeoClient neoClient;

    public NeoController(NasaNeoClient neoClient) {
        this.neoClient = neoClient;
    }

    /**
     * 查询未来 days 天接近地球的小行星列表。
     *
     * @param days 未来天数(1-7,默认 7,超出自动 clamp)
     * @return NeoResponse,按日期分组的近地小行星
     */
    @GetMapping("/feed")
    public ResponseEntity<NeoResponse> getFeed(
            @RequestParam(name = "days", defaultValue = "7") int days) {
        NeoResponse response = neoClient.getFeed(days);
        return ResponseEntity.ok(response);
    }
}
