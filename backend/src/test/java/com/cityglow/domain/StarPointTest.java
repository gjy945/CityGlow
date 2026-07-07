package com.cityglow.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StarPoint record 单元测试。
 *
 * <p>验证:</p>
 * <ul>
 *   <li>accessor 方法返回构造时传入的值</li>
 *   <li>record 自带的 equals/hashCode 行为</li>
 * </ul>
 */
class StarPointTest {

    @Test
    void accessors_returnConstructorValues() {
        StarPoint star = new StarPoint(32349, -1.46, 90.0, 30.5);

        assertThat(star.hip()).isEqualTo(32349);
        assertThat(star.mag()).isEqualTo(-1.46);
        assertThat(star.az()).isEqualTo(90.0);
        assertThat(star.alt()).isEqualTo(30.5);
    }

    @Test
    void equalsAndHashCode_followRecordSemantics() {
        StarPoint a = new StarPoint(32349, -1.46, 90.0, 30.5);
        StarPoint b = new StarPoint(32349, -1.46, 90.0, 30.5);
        StarPoint c = new StarPoint(32349, -1.46, 90.0, 31.0);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }
}
