package com.cityglow.util;

/**
 * 月相描述工具类。
 *
 * <p>把 OpenWeather 的 {@code moon_phase} (0-1) 转换为人类可读的英文描述字符串。</p>
 *
 * <p>OpenWeather moon_phase 取值:</p>
 * <ul>
 *   <li>0 / 1: 新月 (New Moon)</li>
 *   <li>0 - 0.25: 蛾眉月 (Waxing Crescent)</li>
 *   <li>0.25: 上弦月 (First Quarter)</li>
 *   <li>0.25 - 0.5: 盈凸月 (Waxing Gibbous)</li>
 *   <li>0.5: 满月 (Full Moon)</li>
 *   <li>0.5 - 0.75: 亏凸月 (Waning Gibbous)</li>
 *   <li>0.75: 下弦月 (Last Quarter)</li>
 *   <li>0.75 - 1: 残月 (Waning Crescent)</li>
 * </ul>
 */
public final class MoonPhaseDescription {

    private MoonPhaseDescription() {
        // 工具类,禁止实例化
    }

    /**
     * 根据 OpenWeather moon_phase (0-1) 返回英文月相描述。
     *
     * @param moonPhase OpenWeather moon_phase,取值 0-1
     * @return 月相描述(New Moon / Full Moon / First Quarter / Last Quarter /
     *         Waxing Crescent / Waxing Gibbous / Waning Gibbous / Waning Crescent)
     */
    public static String fromPhase(double moonPhase) {
        if (moonPhase == 0 || moonPhase == 1) {
            return "New Moon";
        }
        if (moonPhase == 0.25) {
            return "First Quarter";
        }
        if (moonPhase == 0.5) {
            return "Full Moon";
        }
        if (moonPhase == 0.75) {
            return "Last Quarter";
        }
        if (moonPhase < 0.25) {
            return "Waxing Crescent";
        }
        if (moonPhase < 0.5) {
            return "Waxing Gibbous";
        }
        if (moonPhase < 0.75) {
            return "Waning Gibbous";
        }
        return "Waning Crescent";
    }
}
