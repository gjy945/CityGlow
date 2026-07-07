package com.cityglow.util;

import java.util.Locale;

/**
 * 月相描述工具类。
 *
 * <p>把 OpenWeather 的 {@code moon_phase} (0-1) 转换为人类可读的月相描述字符串,
 * 支持 zh/en/ja 三语言(默认 zh)。</p>
 *
 * <p>OpenWeather moon_phase 取值:</p>
 * <ul>
 *   <li>0 / 1: 新月 (New Moon / 新月)</li>
 *   <li>0 - 0.25: 蛾眉月 (Waxing Crescent / 三日月)</li>
 *   <li>0.25: 上弦月 (First Quarter / 上弦)</li>
 *   <li>0.25 - 0.5: 盈凸月 (Waxing Gibbous / 十三夜月)</li>
 *   <li>0.5: 满月 (Full Moon / 満月)</li>
 *   <li>0.5 - 0.75: 亏凸月 (Waning Gibbous / 十六夜月)</li>
 *   <li>0.75: 下弦月 (Last Quarter / 下弦)</li>
 *   <li>0.75 - 1: 残月 (Waning Crescent / 二十六夜月)</li>
 * </ul>
 */
public final class MoonPhaseDescription {

    /** 月相索引:0=新月,1=蛾眉月,2=上弦,3=盈凸,4=满月,5=亏凸,6=下弦,7=残月。 */
    private static final int NEW_MOON = 0;
    private static final int WAXING_CRESCENT = 1;
    private static final int FIRST_QUARTER = 2;
    private static final int WAXING_GIBBOUS = 3;
    private static final int FULL_MOON = 4;
    private static final int WANING_GIBBOUS = 5;
    private static final int LAST_QUARTER = 6;
    private static final int WANING_CRESCENT = 7;

    private MoonPhaseDescription() {
        // 工具类,禁止实例化
    }

    /**
     * 根据 OpenWeather moon_phase (0-1) 与 Locale 返回对应语言的月相描述。
     *
     * @param moonPhase OpenWeather moon_phase,取值 0-1
     * @param locale    语言(zh/en/ja,其他回退到 zh)
     * @return 月相描述
     */
    public static String fromPhase(double moonPhase, Locale locale) {
        return Messages.moonPhase(phaseIndex(moonPhase), locale);
    }

    /**
     * 根据 OpenWeather moon_phase (0-1) 返回中文月相描述(向后兼容)。
     *
     * @param moonPhase OpenWeather moon_phase,取值 0-1
     * @return 月相描述(中文)
     */
    public static String fromPhase(double moonPhase) {
        return fromPhase(moonPhase, Messages.DEFAULT_LOCALE);
    }

    /**
     * moon_phase (0-1) → 月相索引 0-7。
     *
     * @param moonPhase OpenWeather moon_phase
     * @return 月相索引 0-7
     */
    private static int phaseIndex(double moonPhase) {
        if (moonPhase == 0 || moonPhase == 1) {
            return NEW_MOON;
        }
        if (moonPhase == 0.25) {
            return FIRST_QUARTER;
        }
        if (moonPhase == 0.5) {
            return FULL_MOON;
        }
        if (moonPhase == 0.75) {
            return LAST_QUARTER;
        }
        if (moonPhase < 0.25) {
            return WAXING_CRESCENT;
        }
        if (moonPhase < 0.5) {
            return WAXING_GIBBOUS;
        }
        if (moonPhase < 0.75) {
            return WANING_GIBBOUS;
        }
        return WANING_CRESCENT;
    }
}
