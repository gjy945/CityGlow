package com.cityglow.domain;

/**
 * 观星预报结果(JDK 21 Record,设计文档第 6 节明确要求)。
 *
 * <p>由 ForecastService 计算后返回给 Controller,再序列化给前端。</p>
 *
 * @param score        观星指数 0-100,越高越适合观星
 * @param cloudCover   云量百分比 0-100
 * @param moonPhase    月相描述(New Moon / Full Moon / First Quarter / Last Quarter /
 *                     Waxing Crescent / Waxing Gibbous / Waning Gibbous / Waning Crescent)
 * @param bortleLevel  Bortle 暗空等级 1-9
 * @param message      人类可读消息(如 "今夜极佳!" / "适合观星" / "一般" / "不建议")
 * @param sunrise      日出时刻(unix 秒)
 * @param sunset       日落时刻(unix 秒)
 */
public record ForecastResult(
        int score,
        double cloudCover,
        String moonPhase,
        int bortleLevel,
        String message,
        long sunrise,
        long sunset
) {
}
