package com.cityglow.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenWeatherMap /data/2.5/onecall 响应 DTO(JDK 21 Record)。
 *
 * <p>仅保留 current 字段,用于取日出、日落、月相。</p>
 *
 * <p>注意:OpenWeather 的 {@code moon_phase} 取值 0-1,
 * 0/1=新月, 0.25=上弦(照亮 0.5), 0.5=满月(照亮 1.0), 0.75=下弦(照亮 0.5)。
 * 转换公式见 {@code OpenWeatherClient.toMoonIlluminatedFraction}。</p>
 */
public record OpenWeatherOneCallResponse(
        Current current
) {

    public record Current(
            @JsonProperty("sunrise") long sunrise,
            @JsonProperty("sunset") long sunset,
            @JsonProperty("moon_phase") double moonPhase
    ) {
    }
}
