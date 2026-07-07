package com.cityglow.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SkyViewResult record 单元测试。
 *
 * <p>验证:</p>
 * <ul>
 *   <li>accessor 方法返回构造时传入的值</li>
 *   <li>visibleStars/constellations 为 null 时规范化为空列表(非 null)</li>
 *   <li>传入非空列表时原样保留</li>
 * </ul>
 */
class SkyViewResultTest {

    @Test
    void accessors_returnConstructorValues() {
        List<StarPoint> visible = List.of(new StarPoint(32349, -1.46, 90.0, 30.0));
        List<ConstellationView> constellations = List.of(
                new ConstellationView("orion", "Orion", "猎户座", List.of(), List.of())
        );

        SkyViewResult result = new SkyViewResult(
                visible, constellations, "39.9", "116.4", "2026-07-07", 22
        );

        assertThat(result.visibleStars()).hasSize(1);
        assertThat(result.constellations()).hasSize(1);
        assertThat(result.observerLat()).isEqualTo("39.9");
        assertThat(result.observerLng()).isEqualTo("116.4");
        assertThat(result.date()).isEqualTo("2026-07-07");
        assertThat(result.hour()).isEqualTo(22);
    }

    @Test
    void nullLists_normalizedToEmptyList() {
        SkyViewResult result = new SkyViewResult(null, null, "39.9", "116.4", "2026-07-07", 22);

        assertThat(result.visibleStars()).isNotNull().isEmpty();
        assertThat(result.constellations()).isNotNull().isEmpty();
    }

    @Test
    void nonNullLists_preservedAsIs() {
        List<StarPoint> visible = List.of(new StarPoint(32349, -1.46, 90.0, 30.0));
        List<ConstellationView> constellations = List.of();

        SkyViewResult result = new SkyViewResult(
                visible, constellations, "39.9", "116.4", "2026-07-07", 22
        );

        assertThat(result.visibleStars()).isSameAs(visible);
        assertThat(result.constellations()).isSameAs(constellations);
    }
}
