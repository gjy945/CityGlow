package com.cityglow.domain;

import java.util.List;

/**
 * 星图视图顶层响应(GET /api/v1/sky/constellation-view 的返回)。
 *
 * @param visibleStars  所有可见星(地平线以上,已投影)
 * @param constellations 12 个 MVP 星座的视图列表
 * @param observerLat   观测者纬度
 * @param observerLng   观测者经度
 * @param date          日期(YYYY-MM-DD)
 * @param hour          小时(0-23,本地时区)
 */
public record SkyViewResult(
        List<StarPoint> visibleStars,
        List<ConstellationView> constellations,
        String observerLat,
        String observerLng,
        String date,
        int hour
) {
    public SkyViewResult {
        visibleStars = (visibleStars == null) ? List.of() : visibleStars;
        constellations = (constellations == null) ? List.of() : constellations;
    }
}
