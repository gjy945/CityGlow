package com.cityglow.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Open-Meteo Forecast API 响应 DTO(JDK 21 Record)。
 *
 * <p>Open-Meteo 是免费开源气象 API,无需 API Key,无速率限制,支持商业用途。
 * 文档: https://open-meteo.com/en/docs</p>
 *
 * <p>请求示例:
 * {@code https://api.open-meteo.com/v1/forecast?latitude=39.9&longitude=116.4
 * &current=cloud_cover,temperature_2m,relative_humidity_2m,weather_code
 * &daily=sunrise,sunset&timezone=auto}</p>
 *
 * <p>响应中 sunrise/sunset 是 ISO 8601 字符串(如 "2026-07-07T04:53"),
 * 在 {@link OpenMeteoClient} 中转换为 unix 秒以兼容前端。</p>
 */
public record OpenMeteoResponse(
        Current current,
        Daily daily
) {

    /**
     * 当前气象数据。
     *
     * @param cloudCover         云量百分比 0-100
     * @param temperature2m      2 米温度(摄氏度)
     * @param relativeHumidity2m 2 米相对湿度百分比
     * @param weatherCode        WMO 天气代码(0=晴, 1-3=多云, 45/48=雾, 51-67=雨, 71-77=雪, 80-82=阵雨, 95-99=雷暴)
     */
    public record Current(
            @JsonProperty("cloud_cover") int cloudCover,
            @JsonProperty("temperature_2m") double temperature2m,
            @JsonProperty("relative_humidity_2m") int relativeHumidity2m,
            @JsonProperty("weather_code") int weatherCode
    ) {
    }

    /**
     * 每日数据(数组,索引 0 = 今天)。
     * 只取 sunrise/sunset 第一项(今天)。
     */
    public record Daily(
            @JsonProperty("sunrise") List<String> sunrise,
            @JsonProperty("sunset") List<String> sunset
    ) {
    }

    /**
     * 便捷取今日日出(ISO 8601 字符串),null 或空列表时返回 null。
     */
    public String getTodaySunrise() {
        return (daily != null && daily.sunrise() != null && !daily.sunrise().isEmpty())
                ? daily.sunrise().get(0) : null;
    }

    /**
     * 便捷取今日日落(ISO 8601 字符串),null 或空列表时返回 null。
     */
    public String getTodaySunset() {
        return (daily != null && daily.sunset() != null && !daily.sunset().isEmpty())
                ? daily.sunset().get(0) : null;
    }
}
