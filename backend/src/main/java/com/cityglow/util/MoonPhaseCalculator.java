package com.cityglow.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 月相计算工具(基于已知新月时刻的天文算法)。
 *
 * <p>不依赖外部 API,根据日期推算月相,避免 OpenWeather One Call 3.0
 * 需要单独订阅的问题。</p>
 *
 * <p><b>算法</b>:以 2000-01-06 18:14 UTC(已知新月)为基准,
 * 朔望月周期 29.530588853 天,计算当前时刻距基准的相位差。</p>
 *
 * <p>返回值 moonPhase 取 0-1:</p>
 * <ul>
 *   <li>0 / 1 = 新月(照亮 0)</li>
 *   <li>0.25 = 上弦月(照亮 0.5)</li>
 *   <li>0.5 = 满月(照亮 1.0)</li>
 *   <li>0.75 = 下弦月(照亮 0.5)</li>
 * </ul>
 */
public final class MoonPhaseCalculator {

    /** 已知新月时刻:2000-01-06 18:14 UTC(J2000 历元附近的新月)。 */
    private static final LocalDateTime REFERENCE_NEW_MOON = LocalDateTime.of(2000, 1, 6, 18, 14);

    /** 朔望月平均周期(天)。 */
    private static final double SYNODIC_MONTH = 29.530588853;

    private MoonPhaseCalculator() {
    }

    /**
     * 计算给定日期的月相相位(0-1)。
     *
     * @param date 日期
     * @return 月相相位 0-1(0/1=新月, 0.5=满月)
     */
    public static double calculatePhase(LocalDate date) {
        // 基准新月时刻 → unix 秒
        long refSeconds = REFERENCE_NEW_MOON.toEpochSecond(ZoneOffset.UTC);
        // 输入日期 00:00 UTC → unix 秒(按天精度,忽略时分秒)
        long nowSeconds = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        // 距基准新月的整数天 + 小数天
        double daysSinceNewMoon = (nowSeconds - refSeconds) / 86400.0;
        // 对朔望月周期取余 → 归一化到 [0, 1)
        double phase = (daysSinceNewMoon % SYNODIC_MONTH) / SYNODIC_MONTH;
        // 处理负数(基准前日期),平移到 [0, 1)
        if (phase < 0) {
            phase += 1;
        }
        return phase;
    }

    /**
     * 计算月亮照亮比例(0-1)。
     *
     * <p>公式: {@code (1 - cos(phase * 2π)) / 2}</p>
     *
     * @param phase 月相相位 0-1
     * @return 照亮比例 0-1(0=新月, 1=满月)
     */
    public static double toIlluminatedFraction(double phase) {
        return (1 - Math.cos(phase * 2 * Math.PI)) / 2;
    }
}
