package com.cityglow.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MoonPhaseDescription 单元测试。
 *
 * <p>验证 OpenWeather moon_phase (0-1) → 英文月相描述字符串的转换。</p>
 */
class MoonPhaseDescriptionTest {

    @Test
    void newMoon_atZero() {
        assertThat(MoonPhaseDescription.fromPhase(0.0)).isEqualTo("New Moon");
    }

    @Test
    void newMoon_atOne() {
        // 0 和 1 都等价新月
        assertThat(MoonPhaseDescription.fromPhase(1.0)).isEqualTo("New Moon");
    }

    @Test
    void firstQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.25)).isEqualTo("First Quarter");
    }

    @Test
    void fullMoon() {
        assertThat(MoonPhaseDescription.fromPhase(0.5)).isEqualTo("Full Moon");
    }

    @Test
    void lastQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.75)).isEqualTo("Last Quarter");
    }

    @Test
    void waxingCrescent_betweenNewAndFirstQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.125)).isEqualTo("Waxing Crescent");
    }

    @Test
    void waxingGibbous_betweenFirstQuarterAndFull() {
        assertThat(MoonPhaseDescription.fromPhase(0.375)).isEqualTo("Waxing Gibbous");
    }

    @Test
    void waningGibbous_betweenFullAndLastQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.625)).isEqualTo("Waning Gibbous");
    }

    @Test
    void waningCrescent_betweenLastQuarterAndNew() {
        assertThat(MoonPhaseDescription.fromPhase(0.875)).isEqualTo("Waning Crescent");
    }
}
