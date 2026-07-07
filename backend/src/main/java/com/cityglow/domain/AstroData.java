package com.cityglow.domain;

/**
 * 天文 + 气象聚合数据(JDK 21 Record,设计文档第 6 节明确要求)。
 *
 * <p>由 ForecastService 聚合 OpenWeatherClient 的两个端点结果而来,
 * 作为观星指数计算 {@code StargazingIndex.calculate} 的输入。</p>
 *
 * @param cloudCover                  云量百分比 0-100(直接来自 OpenWeather clouds.all)
 * @param moonIlluminatedFraction     月亮照亮比例 0.0-1.0(已转换,0=新月,1=满月)
 * @param moonPhase                   原始 OpenWeather moon_phase × 1000 取整,便于调试/日志
 * @param sunrise                     日出时刻(unix 秒)
 * @param sunset                      日落时刻(unix 秒)
 */
public record AstroData(
        double cloudCover,
        double moonIlluminatedFraction,
        int moonPhase,
        long sunrise,
        long sunset
) {
}
