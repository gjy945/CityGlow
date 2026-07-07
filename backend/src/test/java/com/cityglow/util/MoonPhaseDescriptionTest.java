package com.cityglow.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MoonPhaseDescription 单元测试。
 *
 * <p>验证 OpenWeather moon_phase (0-1) → 三语言(中文/英文/日文)月相描述字符串的转换。</p>
 */
class MoonPhaseDescriptionTest {

    // ---- 中文(默认)----

    @Test
    void chinese_newMoon_atZero() {
        assertThat(MoonPhaseDescription.fromPhase(0.0, Locale.SIMPLIFIED_CHINESE)).isEqualTo("新月");
    }

    @Test
    void chinese_newMoon_atOne() {
        // 0 和 1 都等价新月
        assertThat(MoonPhaseDescription.fromPhase(1.0, Locale.SIMPLIFIED_CHINESE)).isEqualTo("新月");
    }

    @Test
    void chinese_firstQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.25, Locale.SIMPLIFIED_CHINESE)).isEqualTo("上弦月");
    }

    @Test
    void chinese_fullMoon() {
        assertThat(MoonPhaseDescription.fromPhase(0.5, Locale.SIMPLIFIED_CHINESE)).isEqualTo("满月");
    }

    @Test
    void chinese_lastQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.75, Locale.SIMPLIFIED_CHINESE)).isEqualTo("下弦月");
    }

    @Test
    void chinese_waxingCrescent() {
        assertThat(MoonPhaseDescription.fromPhase(0.125, Locale.SIMPLIFIED_CHINESE)).isEqualTo("蛾眉月");
    }

    @Test
    void chinese_waxingGibbous() {
        assertThat(MoonPhaseDescription.fromPhase(0.375, Locale.SIMPLIFIED_CHINESE)).isEqualTo("盈凸月");
    }

    @Test
    void chinese_waningGibbous() {
        assertThat(MoonPhaseDescription.fromPhase(0.625, Locale.SIMPLIFIED_CHINESE)).isEqualTo("亏凸月");
    }

    @Test
    void chinese_waningCrescent() {
        assertThat(MoonPhaseDescription.fromPhase(0.875, Locale.SIMPLIFIED_CHINESE)).isEqualTo("残月");
    }

    // ---- 英文 ----

    @Test
    void english_newMoon_atZero() {
        assertThat(MoonPhaseDescription.fromPhase(0.0, Locale.ENGLISH)).isEqualTo("New Moon");
    }

    @Test
    void english_newMoon_atOne() {
        assertThat(MoonPhaseDescription.fromPhase(1.0, Locale.ENGLISH)).isEqualTo("New Moon");
    }

    @Test
    void english_firstQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.25, Locale.ENGLISH)).isEqualTo("First Quarter");
    }

    @Test
    void english_fullMoon() {
        assertThat(MoonPhaseDescription.fromPhase(0.5, Locale.ENGLISH)).isEqualTo("Full Moon");
    }

    @Test
    void english_lastQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.75, Locale.ENGLISH)).isEqualTo("Last Quarter");
    }

    @Test
    void english_waxingCrescent() {
        assertThat(MoonPhaseDescription.fromPhase(0.125, Locale.ENGLISH)).isEqualTo("Waxing Crescent");
    }

    @Test
    void english_waxingGibbous() {
        assertThat(MoonPhaseDescription.fromPhase(0.375, Locale.ENGLISH)).isEqualTo("Waxing Gibbous");
    }

    @Test
    void english_waningGibbous() {
        assertThat(MoonPhaseDescription.fromPhase(0.625, Locale.ENGLISH)).isEqualTo("Waning Gibbous");
    }

    @Test
    void english_waningCrescent() {
        assertThat(MoonPhaseDescription.fromPhase(0.875, Locale.ENGLISH)).isEqualTo("Waning Crescent");
    }

    // ---- 日文 ----

    @Test
    void japanese_newMoon_atZero() {
        assertThat(MoonPhaseDescription.fromPhase(0.0, Locale.JAPANESE)).isEqualTo("新月");
    }

    @Test
    void japanese_newMoon_atOne() {
        assertThat(MoonPhaseDescription.fromPhase(1.0, Locale.JAPANESE)).isEqualTo("新月");
    }

    @Test
    void japanese_firstQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.25, Locale.JAPANESE)).isEqualTo("上弦");
    }

    @Test
    void japanese_fullMoon() {
        assertThat(MoonPhaseDescription.fromPhase(0.5, Locale.JAPANESE)).isEqualTo("満月");
    }

    @Test
    void japanese_lastQuarter() {
        assertThat(MoonPhaseDescription.fromPhase(0.75, Locale.JAPANESE)).isEqualTo("下弦");
    }

    @Test
    void japanese_waxingCrescent() {
        assertThat(MoonPhaseDescription.fromPhase(0.125, Locale.JAPANESE)).isEqualTo("三日月");
    }

    @Test
    void japanese_waxingGibbous() {
        assertThat(MoonPhaseDescription.fromPhase(0.375, Locale.JAPANESE)).isEqualTo("十三夜月");
    }

    @Test
    void japanese_waningGibbous() {
        assertThat(MoonPhaseDescription.fromPhase(0.625, Locale.JAPANESE)).isEqualTo("十六夜月");
    }

    @Test
    void japanese_waningCrescent() {
        assertThat(MoonPhaseDescription.fromPhase(0.875, Locale.JAPANESE)).isEqualTo("二十六夜月");
    }

    // ---- 旧签名委托默认中文 ----

    @Test
    void legacyOverload_defaultsToChinese() {
        assertThat(MoonPhaseDescription.fromPhase(0.0)).isEqualTo("新月");
        assertThat(MoonPhaseDescription.fromPhase(0.5)).isEqualTo("满月");
        assertThat(MoonPhaseDescription.fromPhase(0.875)).isEqualTo("残月");
    }
}
