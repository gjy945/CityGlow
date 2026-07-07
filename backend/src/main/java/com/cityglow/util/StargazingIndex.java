package com.cityglow.util;

/**
 * 观星指数计算工具类。
 *
 * <p>综合云量、月相、Bortle 暗空等级计算 0-100 的观星适宜度分数,
 * 并提供分数到消息的映射。</p>
 */
public final class StargazingIndex {

    private StargazingIndex() {
        // 工具类,禁止实例化
    }

    /**
     * 计算观星指数(0-100)。
     *
     * @param cloudCover              云量百分比 0-100
     * @param moonIlluminatedFraction 月亮照亮比例 0.0-1.0(0=新月,1=满月)
     * @param bortleLevel             Bortle 暗空等级 1-9(1=极佳,9=内城天空)
     * @return 观星指数 0-100,越高越适合观星
     */
    public static int calculate(double cloudCover, double moonIlluminatedFraction, int bortleLevel) {
        double score = 100;
        score -= Math.max(0, cloudCover - 20) * 1.5;
        score -= moonIlluminatedFraction * 30;
        score -= (bortleLevel - 1) * 8;
        return (int) Math.round(Math.max(0, Math.min(100, score)));
    }

    /**
     * 根据观星指数返回描述消息。
     *
     * @param score 观星指数 0-100
     * @return 描述消息
     */
    public static String getMessage(int score) {
        if (score >= 80) {
            return "今夜极佳!";
        } else if (score >= 60) {
            return "适合观星";
        } else if (score >= 40) {
            return "一般";
        } else {
            return "不建议";
        }
    }
}
