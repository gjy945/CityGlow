package com.cityglow.service;

import com.cityglow.domain.StarPoint;
import com.cityglow.service.ConstellationDataService.StarRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * StarProjectionService 单元测试。
 *
 * <p>直接 new 实例化(不走 Spring 容器),验证赤道→地平坐标投影算法:</p>
 * <ul>
 *   <li>北极星(Polaris)从北京观测,高度角始终约等于纬度(~40°),任何时间都在地平线以上</li>
 *   <li>猎户座(Orion)冬季从北京可见(夏季夜晚不可见,因 Orion 是冬季星座)</li>
 *   <li>赤道正午(冬至日)猎户座接近反中天,大部分在地平线以下</li>
 *   <li>地平线以下的星被正确过滤</li>
 *   <li>方位角 Az ∈ [0,360),高度角 Alt ∈ [-90,90]</li>
 * </ul>
 *
 * <p><b>关于测试数据</b>:使用真实 HIP 数据 — Polaris HIP 11767(RA=37.95, Dec=89.26),
 * 猎户 7 主星(Betelgeuse/Rigel/Bellatrix/Mintaka/Alnilam/Alnitak/Saiph)。</p>
 */
class StarProjectionServiceTest {

    private final StarProjectionService service = new StarProjectionService();

    // 北京坐标
    private static final double BEIJING_LAT = 39.9;
    private static final double BEIJING_LNG = 116.4;

    // 北极星 HIP 11767
    private static final StarRecord POLARIS =
            new StarRecord(11767, 1.97, 37.95, 89.26, "Polaris");

    // 猎户座 7 颗主星(真实 HIP 数据,取自 bright-stars.json)
    private static final List<StarRecord> ORION_STARS = List.of(
            new StarRecord(27989, 0.50, 88.79, 7.41, "Betelgeuse"),
            new StarRecord(24436, 0.18, 78.63, -8.20, "Rigel"),
            new StarRecord(25336, 1.64, 81.28, 6.35, "Bellatrix"),
            new StarRecord(25930, 2.23, 83.00, -0.30, "Mintaka"),
            new StarRecord(26311, 1.69, 84.05, -1.20, "Alnilam"),
            new StarRecord(26727, 1.77, 85.19, -1.94, "Alnitak"),
            new StarRecord(27366, 2.09, 86.94, -9.67, "Saiph")
    );

    @Test
    void project_polarisFromBeijing_alwaysAboveHorizon() {
        // 北极星 Dec=89.26,对北京(39.9°N)是拱极星(circumpolar),
        // 任何时间都在地平线以上,且高度角始终接近观测者纬度(~40°)
        for (int hour = 0; hour < 24; hour++) {
            List<StarPoint> result = service.project(
                    BEIJING_LAT, BEIJING_LNG,
                    LocalDate.of(2026, 7, 7), hour,
                    List.of(POLARIS));

            assertThat(result).as("hour=" + hour).hasSize(1);
            assertThat(result.get(0).alt()).as("hour=" + hour)
                    .isCloseTo(BEIJING_LAT, within(2.0));
        }
    }

    @Test
    void project_orionFromBeijingInWinter_someStarsVisible() {
        // 猎户座是冬季星座,12 月底 22:00 从北京观测位于高空(Alt 30°+)
        // 注:设计稿原写 2026-07-07(夏季),但夏季夜晚 Orion 在太阳同侧不可见,
        // 此处改为冬至附近(2026-12-22 22:00)以保证至少 1 颗可见
        List<StarPoint> result = service.project(
                BEIJING_LAT, BEIJING_LNG,
                LocalDate.of(2026, 12, 22), 22,
                ORION_STARS);

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void project_sunPositionDec22_starsBelowHorizon() {
        // 12 月 22 日(冬至)太阳 RA≈270°,猎户座 RA≈85° 与太阳几乎相对
        // 赤道(lat=0)正午观测时,猎户座接近反中天(HA≈180°),大部分星在地平线以下
        List<StarPoint> result = service.project(
                0.0, BEIJING_LNG,
                LocalDate.of(2026, 12, 22), 12,
                ORION_STARS);

        assertThat(result).hasSizeLessThan(ORION_STARS.size());
    }

    @Test
    void project_filtersBelowHorizonStars() {
        // 返回的所有星高度角必须 > 0(地平线以下已被过滤)
        List<StarPoint> result = service.project(
                BEIJING_LAT, BEIJING_LNG,
                LocalDate.of(2026, 12, 22), 22,
                ORION_STARS);

        assertThat(result).isNotEmpty();
        for (StarPoint p : result) {
            assertThat(p.alt()).as("hip=" + p.hip()).isGreaterThan(0.0);
        }
    }

    @Test
    void project_emptyInput_returnsEmptyList() {
        List<StarPoint> result = service.project(
                BEIJING_LAT, BEIJING_LNG,
                LocalDate.of(2026, 7, 7), 22,
                List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void project_azimuthInRange0to360() {
        // 所有返回星的方位角必须在 [0, 360)
        List<StarPoint> result = service.project(
                BEIJING_LAT, BEIJING_LNG,
                LocalDate.of(2026, 12, 22), 22,
                ORION_STARS);

        assertThat(result).isNotEmpty();
        for (StarPoint p : result) {
            assertThat(p.az()).as("hip=" + p.hip()).isGreaterThanOrEqualTo(0.0);
            assertThat(p.az()).as("hip=" + p.hip()).isLessThan(360.0);
        }
    }

    @Test
    void project_altitudeInRange_m90to90() {
        // 算法本身保证 Alt ∈ [-90, 90];过滤后返回的 Alt ∈ (0, 90]
        List<StarPoint> result = service.project(
                BEIJING_LAT, BEIJING_LNG,
                LocalDate.of(2026, 12, 22), 22,
                ORION_STARS);

        assertThat(result).isNotEmpty();
        for (StarPoint p : result) {
            assertThat(p.alt()).as("hip=" + p.hip()).isGreaterThanOrEqualTo(-90.0);
            assertThat(p.alt()).as("hip=" + p.hip()).isLessThanOrEqualTo(90.0);
        }
    }

    @Test
    void project_northStarAltitudeEqualsLatitude() {
        // 北极星高度角 ≈ 观测者纬度(允许 ±2°,因 Polaris Dec=89.26 而非 90)
        List<StarPoint> result = service.project(
                BEIJING_LAT, BEIJING_LNG,
                LocalDate.of(2026, 7, 7), 22,
                List.of(POLARIS));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).alt()).isCloseTo(BEIJING_LAT, within(2.0));
    }
}
