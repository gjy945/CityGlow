package com.cityglow.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StargazingIndex 单元测试。
 *
 * <p>覆盖:</p>
 * <ul>
 *   <li>旧签名 calculate(cloud, moon, bortle) 委托新签名(湿度 50、温度 15 默认值)</li>
 *   <li>新签名 calculate(cloud, moon, bortle, humidity, temperatureC) 的湿度/温度扣分</li>
 *   <li>getMessage(score, locale) 三语言</li>
 * </ul>
 */
class StargazingIndexTest {

    // ---- 旧签名委托新签名(湿度 50、温度 15,均不触发新扣分)----

    @Test
    void perfectConditions_returns100() {
        // 新月 + Bortle 1 + 云量 0 + 默认湿度 50 + 默认温度 15 = 满分 100
        assertThat(StargazingIndex.calculate(0, 0.0, 1)).isEqualTo(100);
    }

    @Test
    void worstConditions_returns0() {
        // 满月 + Bortle 9 + 云量 100 → 100 - 80*1.2 - 1*25 - 8*8 = -85 → clamp 0
        assertThat(StargazingIndex.calculate(100, 1.0, 9)).isEqualTo(0);
    }

    @Test
    void cloudCoverWithin20Percent_noDeduction() {
        // 云量 20% 内不扣分:新月 + Bortle 1 + 云量 20 = 100
        assertThat(StargazingIndex.calculate(20, 0.0, 1)).isEqualTo(100);
    }

    @Test
    void cloudCoverAbove20_deducts1_2PerPercent() {
        // 云量 30%: 超出 10%, 扣 12 → 100-12=88(新月 Bortle 1)
        assertThat(StargazingIndex.calculate(30, 0.0, 1)).isEqualTo(88);
    }

    @Test
    void fullMoon_deducts25() {
        // 满月 + 云量 0 + Bortle 1 = 100 - 25 = 75(新算法月相 × 25)
        assertThat(StargazingIndex.calculate(0, 1.0, 1)).isEqualTo(75);
    }

    @Test
    void halfMoon_deducts12_5() {
        // 半月(0.5)+ 云量 0 + Bortle 1 = 100 - 12.5 = 87.5 → 88
        assertThat(StargazingIndex.calculate(0, 0.5, 1)).isEqualTo(88);
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
        // 云量 50% + 半月 0.5 + Bortle 4(默认湿度 50、温度 15,不触发扣分)
        // 100 - (50-20)*1.2 - 0.5*25 - (4-1)*8 = 100 - 36 - 12.5 - 24 = 27.5 → 28
        assertThat(StargazingIndex.calculate(50, 0.5, 4)).isEqualTo(28);
    }

    @Test
    void scoreClampedTo0_whenNegative() {
        // 极差条件: 100 - 大于100 = 负数 → clamp 0
        assertThat(StargazingIndex.calculate(100, 1.0, 9)).isEqualTo(0);
    }

    // ---- 新签名:湿度因素 ----

    @Test
    void humidityBelow70_noDeduction() {
        // 湿度 70% 及以下不扣分:新月 + Bortle 1 + 云量 0 + 湿度 70 + 温度 15 = 100
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 70, 15)).isEqualTo(100);
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 50, 15)).isEqualTo(100);
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 0, 15)).isEqualTo(100);
    }

    @Test
    void humidityAbove70_deducts0_5PerPercent() {
        // 湿度 80%: 超出 10%, 扣 5 → 100 - 5 = 95
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 80, 15)).isEqualTo(95);
        // 湿度 100%: 超出 30%, 扣 15 → 100 - 15 = 85
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 100, 15)).isEqualTo(85);
    }

    // ---- 新签名:温度因素 ----

    @Test
    void temperatureBelow0_deducts5() {
        // 温度 -5°C: 扣 5 → 100 - 5 = 95
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 50, -5)).isEqualTo(95);
    }

    @Test
    void temperatureAbove30_deducts5() {
        // 温度 35°C: 扣 5 → 100 - 5 = 95
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 50, 35)).isEqualTo(95);
    }

    @Test
    void temperatureInNormalRange_noDeduction() {
        // 温度 0°C ~ 30°C 之间不扣分(边界包含)
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 50, 0)).isEqualTo(100);
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 50, 30)).isEqualTo(100);
        assertThat(StargazingIndex.calculate(0, 0.0, 1, 50, 15)).isEqualTo(100);
    }

    @Test
    void combinedHumidityAndTemperature_deductionsStack() {
        // 湿度 80%(扣 5)+ 温度 -5(扣 5)+ Bortle 5(扣 32)= 100 - 5 - 5 - 32 = 58
        assertThat(StargazingIndex.calculate(0, 0.0, 5, 80, -5)).isEqualTo(58);
    }

    // ---- getMessage 测试(三语言)----

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

    @Test
    void getMessage_chineseLocale_returnsChineseMessages() {
        Locale zh = Locale.SIMPLIFIED_CHINESE;
        assertThat(StargazingIndex.getMessage(85, zh)).isEqualTo("今夜极佳!");
        assertThat(StargazingIndex.getMessage(65, zh)).isEqualTo("适合观星");
        assertThat(StargazingIndex.getMessage(45, zh)).isEqualTo("一般");
        assertThat(StargazingIndex.getMessage(20, zh)).isEqualTo("不建议");
    }

    @Test
    void getMessage_englishLocale_returnsEnglishMessages() {
        Locale en = Locale.ENGLISH;
        assertThat(StargazingIndex.getMessage(85, en)).isEqualTo("Excellent tonight!");
        assertThat(StargazingIndex.getMessage(65, en)).isEqualTo("Good for stargazing");
        assertThat(StargazingIndex.getMessage(45, en)).isEqualTo("Fair");
        assertThat(StargazingIndex.getMessage(20, en)).isEqualTo("Not recommended");
    }

    @Test
    void getMessage_japaneseLocale_returnsJapaneseMessages() {
        Locale ja = Locale.JAPANESE;
        assertThat(StargazingIndex.getMessage(85, ja)).isEqualTo("今夜は最高!");
        assertThat(StargazingIndex.getMessage(65, ja)).isEqualTo("観星に適す");
        assertThat(StargazingIndex.getMessage(45, ja)).isEqualTo("普通");
        assertThat(StargazingIndex.getMessage(20, ja)).isEqualTo("お勧めしない");
    }
}
