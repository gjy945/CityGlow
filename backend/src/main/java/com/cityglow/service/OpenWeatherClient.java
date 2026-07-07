package com.cityglow.service;

import com.cityglow.domain.OpenWeatherCurrentResponse;
import com.cityglow.domain.OpenWeatherOneCallResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenWeatherMap API 客户端(Spring 6.1 RestClient)。
 *
 * <p>封装两个端点:</p>
 * <ul>
 *   <li>{@code /data/2.5/weather}:取当前云量、温度、湿度、天气描述。</li>
 *   <li>{@code /data/2.5/onecall}:取日出、日落、月相。</li>
 * </ul>
 *
 * <p>API Key 从 {@code application.yml} 的 {@code openweather.api.key}
 * 注入,严禁硬编码。</p>
 *
 * <p>月相转换:OpenWeather 的 {@code moon_phase} 取值 0-1,
 * 0/1=新月, 0.25=上弦, 0.5=满月, 0.75=下弦。
 * 转换为照亮比例用 {@link #toMoonIlluminatedFraction(double)}。</p>
 */
@Component
public class OpenWeatherClient {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";

    private final RestClient restClient;
    private final String apiKey;

    public OpenWeatherClient(
            RestClient.Builder restClientBuilder,
            @Value("${openweather.api.key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
    }

    /**
     * 取当前天气(含云量)。
     *
     * @param lat 纬度
     * @param lng 经度
     */
    public OpenWeatherCurrentResponse getCurrentWeather(double lat, double lng) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lng)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .queryParam("lang", "zh_cn")
                        .build())
                .retrieve()
                .body(OpenWeatherCurrentResponse.class);
    }

    /**
     * 取天文数据(日出、日落、月相)。
     *
     * @param lat 纬度
     * @param lng 经度
     */
    public OpenWeatherOneCallResponse getOneCall(double lat, double lng) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/onecall")
                        .queryParam("lat", lat)
                        .queryParam("lon", lng)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .queryParam("exclude", "minutely,hourly,daily,alerts")
                        .queryParam("lang", "zh_cn")
                        .build())
                .retrieve()
                .body(OpenWeatherOneCallResponse.class);
    }

    /**
     * 将 OpenWeather moon_phase (0-1) 转换为月亮照亮比例 (0-1)。
     *
     * <p>公式: {@code (1 - cos(moon_phase * 2π)) / 2}</p>
     * <ul>
     *   <li>0 → 0(新月)</li>
     *   <li>0.25 → 0.5(上弦)</li>
     *   <li>0.5 → 1.0(满月)</li>
     *   <li>0.75 → 0.5(下弦)</li>
     *   <li>1 → 0(新月)</li>
     * </ul>
     */
    public static double toMoonIlluminatedFraction(double moonPhase) {
        return (1 - Math.cos(moonPhase * 2 * Math.PI)) / 2;
    }
}
