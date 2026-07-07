package com.cityglow.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConstellationView record 单元测试。
 *
 * <p>验证:</p>
 * <ul>
 *   <li>accessor 方法返回构造时传入的值</li>
 *   <li>stars/lines 为 null 时规范化为空列表(非 null)</li>
 *   <li>传入非空列表时原样保留</li>
 * </ul>
 */
class ConstellationViewTest {

    @Test
    void accessors_returnConstructorValues() {
        List<StarPoint> stars = List.of(
                new StarPoint(27989, 0.5, 10.0, 20.0),
                new StarPoint(26727, 1.6, 15.0, 25.0)
        );
        List<int[]> lines = List.of(new int[]{0, 1});

        ConstellationView view = new ConstellationView("orion", "Orion", "猎户座", stars, lines);

        assertThat(view.name()).isEqualTo("orion");
        assertThat(view.latin()).isEqualTo("Orion");
        assertThat(view.chinese()).isEqualTo("猎户座");
        assertThat(view.stars()).hasSize(2);
        assertThat(view.lines()).hasSize(1);
    }

    @Test
    void nullStars_normalizedToEmptyList() {
        ConstellationView view = new ConstellationView("orion", "Orion", "猎户座", null, null);

        assertThat(view.stars()).isNotNull().isEmpty();
        assertThat(view.lines()).isNotNull().isEmpty();
    }

    @Test
    void nonNullLists_preservedAsIs() {
        List<StarPoint> stars = List.of(new StarPoint(27989, 0.5, 10.0, 20.0));
        List<int[]> lines = List.of(new int[]{0, 0});

        ConstellationView view = new ConstellationView("orion", "Orion", "猎户座", stars, lines);

        assertThat(view.stars()).isSameAs(stars);
        assertThat(view.lines()).isSameAs(lines);
    }
}
