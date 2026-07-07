package com.cityglow.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BortleEstimator 单元测试。
 *
 * <p>验证已知暗夜公园/天文台坐标附近返回低 Bortle 值,
 * 其他位置返回默认中等光污染值 5。同时覆盖三语言描述。</p>
 *
 * <p>注意:这是粗粒度查表的简化实现,未来可接入真实光污染数据源
 * (如 Light Pollution Map 瓦片),届时测试需相应调整。</p>
 */
class BortleEstimatorTest {

    @Test
    void nearAliObservatory_returnsBortle1() {
        // 阿里天文台(西藏噶尔): 32.5, 80.0 → Bortle 1(极佳暗空)
        assertThat(BortleEstimator.estimate(32.5, 80.0)).isEqualTo(1);
    }

    @Test
    void nearAliObservatory_withinTolerance_returnsBortle1() {
        // 容差 2 度内仍判定为阿里天文台附近
        assertThat(BortleEstimator.estimate(33.0, 79.5)).isEqualTo(1);
    }

    @Test
    void nearXinglongObservatory_returnsBortle3() {
        // 兴隆天文台(河北): 40.4, 117.6 → Bortle 3
        assertThat(BortleEstimator.estimate(40.4, 117.6)).isEqualTo(3);
    }

    @Test
    void nearXinglongObservatory_withinTolerance_returnsBortle3() {
        // 容差 1 度内仍判定为兴隆天文台附近
        assertThat(BortleEstimator.estimate(40.9, 118.0)).isEqualTo(3);
    }

    @Test
    void beijingCityCenter_returnsDefaultBortle5() {
        // 北京市中心: 39.9, 116.4 → 默认中等光污染 5
        assertThat(BortleEstimator.estimate(39.9, 116.4)).isEqualTo(5);
    }

    @Test
    void arbitraryLocation_returnsDefaultBortle5() {
        // 任意非暗夜公园位置 → 默认 5
        assertThat(BortleEstimator.estimate(0.0, 0.0)).isEqualTo(5);
        assertThat(BortleEstimator.estimate(-33.8, 151.2)).isEqualTo(5); // 悉尼
        assertThat(BortleEstimator.estimate(35.6, 139.6)).isEqualTo(5); // 东京
    }

    @Test
    void estimate_alwaysInRange1to9() {
        // 任何坐标都应返回 1-9 的合法 Bortle 值
        double[][] samples = {
                {0, 0}, {90, 180}, {-90, -180}, {32.5, 80.0},
                {40.4, 117.6}, {39.9, 116.4}, {50, 50}
        };
        for (double[] s : samples) {
            int b = BortleEstimator.estimate(s[0], s[1]);
            assertThat(b).isBetween(1, 9);
        }
    }

    // ---- getDescription 三语言测试 ----

    @Test
    void getDescription_chinese_returnsChineseDescriptions() {
        Locale zh = Locale.SIMPLIFIED_CHINESE;
        assertThat(BortleEstimator.getDescription(1, zh)).isEqualTo("极佳暗空");
        assertThat(BortleEstimator.getDescription(2, zh)).isEqualTo("暗空良好");
        assertThat(BortleEstimator.getDescription(3, zh)).isEqualTo("暗空良好");
        assertThat(BortleEstimator.getDescription(4, zh)).isEqualTo("郊区天空");
        assertThat(BortleEstimator.getDescription(5, zh)).isEqualTo("郊区天空");
        assertThat(BortleEstimator.getDescription(6, zh)).isEqualTo("郊区");
        assertThat(BortleEstimator.getDescription(7, zh)).isEqualTo("郊区");
        assertThat(BortleEstimator.getDescription(8, zh)).isEqualTo("城市");
        assertThat(BortleEstimator.getDescription(9, zh)).isEqualTo("城市");
    }

    @Test
    void getDescription_english_returnsEnglishDescriptions() {
        Locale en = Locale.ENGLISH;
        assertThat(BortleEstimator.getDescription(1, en)).isEqualTo("Excellent dark sky");
        assertThat(BortleEstimator.getDescription(3, en)).isEqualTo("Good dark sky");
        assertThat(BortleEstimator.getDescription(5, en)).isEqualTo("Suburban sky");
        assertThat(BortleEstimator.getDescription(7, en)).isEqualTo("Bright suburban");
        assertThat(BortleEstimator.getDescription(9, en)).isEqualTo("City sky");
    }

    @Test
    void getDescription_japanese_returnsJapaneseDescriptions() {
        Locale ja = Locale.JAPANESE;
        assertThat(BortleEstimator.getDescription(1, ja)).isEqualTo("素晴らしい暗夜");
        assertThat(BortleEstimator.getDescription(3, ja)).isEqualTo("良好な暗夜");
        assertThat(BortleEstimator.getDescription(5, ja)).isEqualTo("郊外の空");
        assertThat(BortleEstimator.getDescription(7, ja)).isEqualTo("明るい郊外");
        assertThat(BortleEstimator.getDescription(9, ja)).isEqualTo("都市の空");
    }
}
