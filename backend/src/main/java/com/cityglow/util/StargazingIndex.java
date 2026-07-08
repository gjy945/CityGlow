package com.cityglow.util;

import java.util.Locale;

/**
 * 观星指数计算工具类。
 *
 * <p>综合云量、月相、Bortle 暗空等级、湿度、温度计算 0-100 的观星适宜度分数,
 * 并提供分数到消息的映射(支持 zh/en/ja 三语言)。</p>
 *
 * <p><b>扣分规则</b>:</p>
 * <ul>
 *   <li>云量:≤20% 不扣,>20% 每个百分点扣 1.2</li>
 *   <li>月相:照亮比例 × 25(月相可预报,故比早期 30 略宽松)</li>
 *   <li>Bortle:(bortleLevel - 1) × 8</li>
 *   <li>湿度:&gt;70% 开始扣分,每超 1% 扣 0.5</li>
 *   <li>温度:&lt;0°C 或 &gt;30°C 扣 5 分(极端温度不适合观测)</li>
 * </ul>
 */
public final class StargazingIndex {

    /** 旧方法签名委托新方法时使用的默认湿度(50%,不触发湿度扣分)。 */
    private static final double DEFAULT_HUMIDITY = 50.0;

    /** 旧方法签名委托新方法时使用的默认温度(15°C,不触发温度扣分)。 */
    private static final double DEFAULT_TEMPERATURE_C = 15.0;

    private StargazingIndex() {
        // 工具类,禁止实例化
    }

    /**
     * 计算观星指数(0-100),含湿度与温度因素。
     *
     * @param cloudCover              云量百分比 0-100
     * @param moonIlluminatedFraction 月亮照亮比例 0.0-1.0(0=新月,1=满月)
     * @param bortleLevel             Bortle 暗空等级 1-9(1=极佳,9=内城天空)
     * @param humidity                相对湿度百分比 0-100
     * @param temperatureC            摄氏温度
     * @return 观星指数 0-100,越高越适合观星
     */
    public static int calculate(double cloudCover, double moonIlluminatedFraction,
                                int bortleLevel, double humidity, double temperatureC) {
        double score = 100;
        // 云量:≤20% 不扣,>20% 每个百分点扣 1.2
        score -= Math.max(0, cloudCover - 20) * 1.2;
        // 月相:照亮比例 × 25
        score -= moonIlluminatedFraction * 25;
        // Bortle:(level - 1) × 8
        score -= (bortleLevel - 1) * 8;
        // 湿度:>70% 开始扣分,每超 1% 扣 0.5
        score -= Math.max(0, humidity - 70) * 0.5;
        // 温度:<0°C 或 >30°C 扣 5 分(极端温度不适合观测)
        if (temperatureC < 0 || temperatureC > 30) {
            score -= 5;
        }
        // 钳制到 [0, 100] 后四舍五入为整数,确保返回值始终在合法区间
        return (int) Math.round(Math.max(0, Math.min(100, score)));
    }

    /**
     * 计算观星指数(旧签名,向后兼容)。
     *
     * <p>委托新方法,湿度默认 50、温度默认 15(均不触发新扣分规则)。</p>
     *
     * @param cloudCover              云量百分比 0-100
     * @param moonIlluminatedFraction 月亮照亮比例 0.0-1.0
     * @param bortleLevel             Bortle 暗空等级 1-9
     * @return 观星指数 0-100
     */
    public static int calculate(double cloudCover, double moonIlluminatedFraction, int bortleLevel) {
        return calculate(cloudCover, moonIlluminatedFraction, bortleLevel,
                DEFAULT_HUMIDITY, DEFAULT_TEMPERATURE_C);
    }

    /**
     * 根据观星指数返回描述消息(支持多语言)。
     *
     * @param score  观星指数 0-100
     * @param locale 语言(zh/en/ja,其他回退到 zh)
     * @return 描述消息
     */
    public static String getMessage(int score, Locale locale) {
        return Messages.stargazingMessage(score, locale);
    }

    /**
     * 根据观星指数返回描述消息(默认中文,向后兼容)。
     *
     * @param score 观星指数 0-100
     * @return 描述消息
     */
    public static String getMessage(int score) {
        return getMessage(score, Messages.DEFAULT_LOCALE);
    }
}
