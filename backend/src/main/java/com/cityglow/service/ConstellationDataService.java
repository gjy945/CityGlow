package com.cityglow.service;

import com.cityglow.domain.MythCard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * 星座静态数据服务。
 *
 * <p>启动时(@PostConstruct)加载 classpath:data/ 下的 3 个 JSON 文件到内存:
 * <ul>
 *   <li>bright-stars.json - 亮星表(赤道坐标 RA/Dec,未投影)</li>
 *   <li>constellations.json - 12 星座连线(引用 HIP 编号)</li>
 *   <li>myths.json - 24 篇神话故事</li>
 * </ul>
 *
 * <p>加载后提供同步查询接口,所有数据只读。</p>
 *
 * <p><b>内部数据结构</b>:</p>
 * <ul>
 *   <li>{@code hipToStar}: Map<Integer, StarRecord> — HIP → 原始星(ra/dec)</li>
 *   <li>{@code constellations}: Map<String, ConstellationRecord> — name(如 "orion") → 星座数据</li>
 *   <li>{@code myths}: Map<String, Map<String, MythCard>> — constellation → culture → MythCard</li>
 * </ul>
 */
@Service
public class ConstellationDataService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** HIP → 原始星数据(赤道坐标,未投影) */
    private Map<Integer, StarRecord> hipToStar;

    /** 星座名(如 "orion") → 星座原始数据 */
    private Map<String, ConstellationRecord> constellations;

    /** constellation → culture → 神话卡 */
    private Map<String, Map<String, MythCard>> myths;

    /**
     * 启动时加载 3 个 JSON 文件。
     *
     * <p>失败时抛 RuntimeException,阻止应用启动(数据缺失无法工作)。</p>
     */
    @PostConstruct
    void load() {
        try {
            hipToStar = loadBrightStars();
            constellations = loadConstellations();
            myths = loadMyths();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load constellation data", e);
        }
    }

    // ===== 查询接口 =====

    /** 获取所有星座名(12 个,如 "orion", "ursaMajor" 等)。 */
    public Set<String> getAllConstellationNames() {
        return Collections.unmodifiableSet(constellations.keySet());
    }

    /** 按 name 获取星座原始数据(含 starHips 和 lines)。 */
    public Optional<ConstellationRecord> getConstellation(String name) {
        return Optional.ofNullable(constellations.get(name));
    }

    /** 获取所有星座原始数据。 */
    public Collection<ConstellationRecord> getAllConstellations() {
        return Collections.unmodifiableCollection(constellations.values());
    }

    /** 按 HIP 获取原始星(赤道坐标)。 */
    public Optional<StarRecord> getStar(int hip) {
        return Optional.ofNullable(hipToStar.get(hip));
    }

    /** 按 HIP 批量获取原始星。 */
    public List<StarRecord> getStars(Collection<Integer> hips) {
        List<StarRecord> result = new ArrayList<>();
        for (int hip : hips) {
            StarRecord s = hipToStar.get(hip);
            if (s != null) result.add(s);
        }
        return result;
    }

    /** 获取某星座的神话故事(指定文化)。 */
    public Optional<MythCard> getMyth(String constellation, String culture) {
        Map<String, MythCard> byCulture = myths.get(constellation);
        if (byCulture == null) return Optional.empty();
        return Optional.ofNullable(byCulture.get(culture));
    }

    /** 获取某星座的全部神话(希腊 + 中国,2 篇)。 */
    public List<MythCard> getMyths(String constellation) {
        Map<String, MythCard> byCulture = myths.get(constellation);
        if (byCulture == null) return List.of();
        return List.copyOf(byCulture.values());
    }

    // ===== 加载方法 =====

    private Map<Integer, StarRecord> loadBrightStars() throws Exception {
        try (InputStream is = new ClassPathResource("data/bright-stars.json").getInputStream()) {
            List<StarRecord> stars = objectMapper.readValue(is, new TypeReference<>() {});
            Map<Integer, StarRecord> map = new HashMap<>();
            for (StarRecord s : stars) map.put(s.hip(), s);
            return Collections.unmodifiableMap(map);
        }
    }

    private Map<String, ConstellationRecord> loadConstellations() throws Exception {
        try (InputStream is = new ClassPathResource("data/constellations.json").getInputStream()) {
            List<ConstellationRecord> list = objectMapper.readValue(is, new TypeReference<>() {});
            Map<String, ConstellationRecord> map = new HashMap<>();
            for (ConstellationRecord c : list) map.put(c.name(), c);
            return Collections.unmodifiableMap(map);
        }
    }

    private Map<String, Map<String, MythCard>> loadMyths() throws Exception {
        try (InputStream is = new ClassPathResource("data/myths.json").getInputStream()) {
            List<MythCard> list = objectMapper.readValue(is, new TypeReference<>() {});
            Map<String, Map<String, MythCard>> map = new HashMap<>();
            for (MythCard m : list) {
                map.computeIfAbsent(m.constellation(), k -> new HashMap<>())
                   .put(m.culture(), m);
            }
            // 转不可变
            Map<String, Map<String, MythCard>> immutable = new HashMap<>();
            for (var e : map.entrySet()) {
                immutable.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
            }
            return Collections.unmodifiableMap(immutable);
        }
    }

    // ===== 内部 record(对应 JSON 结构) =====

    /** 亮星表中的一行(赤道坐标,未投影)。 */
    public record StarRecord(int hip, double mag, double ra, double dec, String name) {}

    /** 星座原始数据(JSON 直接映射)。 */
    public record ConstellationRecord(
            String name,
            String latin,
            String chinese,
            List<Integer> starHips,
            List<int[]> lines
    ) {
        public ConstellationRecord {
            starHips = (starHips == null) ? List.of() : starHips;
            lines = (lines == null) ? List.of() : lines;
        }
    }
}
