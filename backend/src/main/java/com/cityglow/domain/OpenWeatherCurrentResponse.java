package com.cityglow.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenWeatherMap /data/2.5/weather 响应 DTO(JDK 21 Record)。
 *
 * <p>只保留观星指数计算需要的字段:云量、温度、湿度、天气描述。</p>
 *
 * <p>JSON 字段映射:OpenWeather 返回的 snake_case 字段通过
 * {@link JsonProperty} 显式映射到 record 组件(Jackson 默认不会自动
 * 把 {@code moon_phase} 这种下划线字段映射到 {@code moonPhase})。</p>
 */
public record OpenWeatherCurrentResponse(
        Clouds clouds,
        Main main,
        List<Weather> weather
) {

    /**
     * 云量。
     *
     * @param all 云量百分比 0-100
     */
    public record Clouds(int all) {
    }

    /**
     * 主要气象指标。
     */
    public record Main(
            @JsonProperty("temp") double temp,
            @JsonProperty("humidity") int humidity
    ) {
    }

    /**
     * 天气描述(数组,取第一个的 description)。
     */
    public record Weather(
            @JsonProperty("description") String description
    ) {
    }

    /**
     * 便捷取云量:当 clouds 为 null(JSON 缺字段)时返回 0。
     */
    public int getCloudCover() {
        return clouds() != null ? clouds().all() : 0;
    }
}
