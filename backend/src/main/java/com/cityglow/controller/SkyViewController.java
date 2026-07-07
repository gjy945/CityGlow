package com.cityglow.controller;

import com.cityglow.domain.ConstellationView;
import com.cityglow.domain.MythCard;
import com.cityglow.domain.SkyViewResult;
import com.cityglow.domain.StarPoint;
import com.cityglow.service.ConstellationDataService;
import com.cityglow.service.ConstellationDataService.ConstellationRecord;
import com.cityglow.service.ConstellationDataService.StarRecord;
import com.cityglow.service.StarProjectionService;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 星图视图 REST 接口。
 *
 * <p>暴露两个端点:</p>
 * <ul>
 *   <li>{@code GET /api/v1/sky/constellation-view?lat=&lng=&date=&hour=}
 *       - 返回当晚当地可见星空 + 12 星座连线视图</li>
 *   <li>{@code GET /api/v1/sky/myths/{constellation}}
 *       - 返回该星座的希腊 + 中国神话故事卡(2 篇)</li>
 * </ul>
 *
 * <p>两个端点都是公开接口(SecurityConfig 放行 {@code /api/v1/sky/**})。</p>
 *
 * <p><b>缓存</b>:constellation-view 按 (lat,lng,date,hour) 缓存 1 小时。
 * myths 是静态数据,ConstellationDataService 启动时已加载到内存,无需额外缓存。</p>
 */
@RestController
@RequestMapping("/api/v1/sky")
public class SkyViewController {

    private final ConstellationDataService dataService;
    private final StarProjectionService projectionService;
    private final Cache<String, SkyViewResult> skyViewCache;

    public SkyViewController(
            ConstellationDataService dataService,
            StarProjectionService projectionService,
            @Qualifier("skyViewCache") Cache<String, SkyViewResult> skyViewCache) {
        this.dataService = dataService;
        this.projectionService = projectionService;
        this.skyViewCache = skyViewCache;
    }

    /**
     * 查询星图视图(可见星 + 12 星座连线)。
     *
     * @param lat  纬度
     * @param lng  经度
     * @param date 日期(YYYY-MM-DD,默认今天)
     * @param hour 小时(0-23,默认 22 即晚上 10 点)
     * @return SkyViewResult
     */
    @GetMapping("/constellation-view")
    public ResponseEntity<SkyViewResult> getSkyView(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(name = "date", required = false) String date,
            @RequestParam(name = "hour", defaultValue = "22") int hour) {

        LocalDate parsedDate = (date == null || date.isBlank())
                ? LocalDate.now() : LocalDate.parse(date);
        int safeHour = (hour < 0 || hour > 23) ? 22 : hour;

        String cacheKey = formatKey(lat, lng, parsedDate, safeHour);
        SkyViewResult result = skyViewCache.get(cacheKey, k ->
                buildSkyView(lat, lng, parsedDate, safeHour));
        return ResponseEntity.ok(result);
    }

    /**
     * 查询某星座的神话故事卡(希腊 + 中国,2 篇)。
     *
     * @param constellation 星座名(如 "orion")
     * @return 神话卡列表(2 篇)
     */
    @GetMapping("/myths/{constellation}")
    public ResponseEntity<List<MythCard>> getMyths(
            @PathVariable String constellation) {
        List<MythCard> myths = dataService.getMyths(constellation);
        if (myths.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(myths);
    }

    // ===== 内部方法 =====

    private SkyViewResult buildSkyView(double lat, double lng, LocalDate date, int hour) {
        // 1. 投影所有星到地平坐标
        List<StarRecord> allStars = new ArrayList<>(dataService.getAllConstellations())
                .stream()
                .flatMap(c -> dataService.getStars(c.starHips()).stream())
                .toList();
        List<StarPoint> visibleStars = projectionService.project(lat, lng, date, hour, allStars);

        // 2. 构建每个星座的视图(只保留可见星的连线)
        List<ConstellationView> constellationViews = new ArrayList<>();
        for (ConstellationRecord cr : dataService.getAllConstellations()) {
            List<StarRecord> crStars = dataService.getStars(cr.starHips());
            List<StarPoint> projected = projectionService.project(lat, lng, date, hour, crStars);
            // 只保留可见星
            List<StarPoint> visible = projected.stream()
                    .filter(s -> s.alt() > 0)
                    .toList();
            if (visible.isEmpty()) {
                continue;  // 整个星座在地平线以下,跳过
            }
            // 重新映射连线索引:projected 顺序与 crStars 一致,过滤后索引会变
            List<int[]> remappedLines = remapLines(crStars, projected, cr.lines());
            constellationViews.add(new ConstellationView(
                    cr.name(), cr.latin(), cr.chinese(), visible, remappedLines));
        }

        return new SkyViewResult(
                visibleStars,
                constellationViews,
                String.format("%.4f", lat),
                String.format("%.4f", lng),
                date.toString(),
                hour);
    }

    /**
     * 重新映射连线索引。
     *
     * <p>原 lines 引用 starHips 的索引,project 后部分星可能不可见(alt ≤ 0 被过滤)。
     * 这里构建新索引:只保留两端都可见的连线,索引基于过滤后的 visible 列表。</p>
     *
     * @param originalStars 星座原始星列表(赤道坐标,含可能不可见的星)
     * @param projected     投影后的星点列表(StarProjectionService 已过滤 alt ≤ 0)
     * @param originalLines 原始连线索引对,每条 [i, j] 引用 originalStars 的索引
     * @return 重新映射后的连线索引对,引用 visible 列表的索引
     */
    private List<int[]> remapLines(List<StarRecord> originalStars,
                                   List<StarPoint> projected,
                                   List<int[]> originalLines) {
        // 构建 原始索引 → 过滤后索引 的映射
        // projected 包含可见的星,顺序与 originalStars 一致(只是过滤了 alt ≤ 0)
        Map<Integer, Integer> oldToNew = new HashMap<>();
        int newIdx = 0;
        for (int i = 0; i < originalStars.size(); i++) {
            int hip = originalStars.get(i).hip();
            boolean visible = projected.stream().anyMatch(p -> p.hip() == hip);
            if (visible) {
                oldToNew.put(i, newIdx);
                newIdx++;
            }
        }

        List<int[]> result = new ArrayList<>();
        for (int[] line : originalLines) {
            Integer newI = oldToNew.get(line[0]);
            Integer newJ = oldToNew.get(line[1]);
            if (newI != null && newJ != null) {
                result.add(new int[]{newI, newJ});
            }
        }
        return result;
    }

    private String formatKey(double lat, double lng, LocalDate date, int hour) {
        return String.format(Locale.ROOT, "%.4f,%.4f,%s,%d", lat, lng, date, hour);
    }
}
