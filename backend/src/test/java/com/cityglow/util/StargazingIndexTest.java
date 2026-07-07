package com.cityglow.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StargazingIndexTest {

    @Test
    void perfectConditions_returns100() {
        // 新月 + Bortle 1 + 云量 0 = 满分 100
        assertThat(StargazingIndex.calculate(0, 0.0, 1)).isEqualTo(100);
    }

    @Test
    void worstConditions_returns0() {
        // 满月 + Bortle 9 + 云量 100 = 0(会被 clamp 到 0)
        // 100 - (100-20)*1.5 - 1.0*30 - (9-1)*8 = 100 - 120 - 30 - 64 = -114 → clamp 0
        assertThat(StargazingIndex.calculate(100, 1.0, 9)).isEqualTo(0);
    }

    @Test
    void cloudCoverWithin20Percent_noDeduction() {
        // 云量 20% 内不扣分:新月 + Bortle 1 + 云量 20 = 100
        assertThat(StargazingIndex.calculate(20, 0.0, 1)).isEqualTo(100);
    }

    @Test
    void cloudCoverAbove20_deducts1_5PerPercent() {
        // 云量 30%: 超出 10%, 扣 15 → 100-15=85(新月 Bortle 1)
        assertThat(StargazingIndex.calculate(30, 0.0, 1)).isEqualTo(85);
    }

    @Test
    void fullMoon_deducts30() {
        // 满月 + 新月云量 0 + Bortle 1 = 100 - 30 = 70
        assertThat(StargazingIndex.calculate(0, 1.0, 1)).isEqualTo(70);
    }

    @Test
    void halfMoon_deducts15() {
        // 半月(0.5)+ 云量 0 + Bortle 1 = 100 - 15 = 85
        assertThat(StargazingIndex.calculate(0, 0.5, 1)).isEqualTo(85);
    }

    @Test
    void bortleLevel9_deducts64() {
        // Bortle 9 + 新月 + 云量 0 = 100 - 64 = 36
        assertThat(StargazingIndex.calculate(0, 0.0, 9)).isEqualTo(36);
    }

    @Test
    void bortleLevel5_deducts32() {
        // Bortle 5 + 新月 + 云量 0 = 100 - 32 = 68
        assertThat(StargazingIndex.calculate(0, 0.0, 5)).isEqualTo(68);
    }

    @Test
    void combinedConditions_correctScore() {
        // 云量 50% + 半月 0.5 + Bortle 4
        // 100 - (50-20)*1.5 - 0.5*30 - (4-1)*8 = 100 - 45 - 15 - 24 = 16
        assertThat(StargazingIndex.calculate(50, 0.5, 4)).isEqualTo(16);
    }

    @Test
    void scoreClampedTo0_whenNegative() {
        // 极差条件: 100 - 大于100 = 负数 → clamp 0
        assertThat(StargazingIndex.calculate(100, 1.0, 9)).isEqualTo(0);
    }

    // ---- getMessage 测试 ----

    @Test
    void getMessage_excellent_whenScoreAbove80() {
        assertThat(StargazingIndex.getMessage(85)).isEqualTo("今夜极佳!");
        assertThat(StargazingIndex.getMessage(100)).isEqualTo("今夜极佳!");
        assertThat(StargazingIndex.getMessage(80)).isEqualTo("今夜极佳!");
    }

    @Test
    void getMessage_good_whenScore60to79() {
        assertThat(StargazingIndex.getMessage(79)).isEqualTo("适合观星");
        assertThat(StargazingIndex.getMessage(60)).isEqualTo("适合观星");
    }

    @Test
    void getMessage_average_whenScore40to59() {
        assertThat(StargazingIndex.getMessage(59)).isEqualTo("一般");
        assertThat(StargazingIndex.getMessage(40)).isEqualTo("一般");
    }

    @Test
    void getMessage_poor_whenScoreBelow40() {
        assertThat(StargazingIndex.getMessage(39)).isEqualTo("不建议");
        assertThat(StargazingIndex.getMessage(0)).isEqualTo("不建议");
    }
}
