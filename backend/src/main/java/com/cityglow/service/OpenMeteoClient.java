package com.cityglow.service;

import com.cityglow.domain.OpenMeteoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Open-Meteo API 客户端(Spring 6.1 RestClient)。
 *
 * <p>Open-Meteo 是免费开源气象 API,<b>无需 API Key</b>,无速率限制,支持商业用途。
 * 文档: https://open-meteo.com/en/docs</p>
 *
 * <p>封装一个端点 {@code /v1/forecast},一次请求取当前云量 + 每日日出日落。
 * 月相不从此 API 取(Open-Meteo 无月相字段),改用
 * {@link com.cityglow.util.MoonPhaseCalculator} 天文算法计算。</p>
 *
 * <p>响应中 sunrise/sunset 是 ISO 8601 字符串(如 "2026-07-07T04:53"),
 * 通过 {@link #isoToEpochSecond(String)} 转换为 unix 秒以兼容前端。</p>
 */
@Component
public class OpenMeteoClient {

    private static final String BASE_URL = "https://api.open-meteo.com/v1";

    /** ISO 8601 本地时间格式(Open-Meteo 返回 "2026-07-07T04:53")。 */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RestClient restClient;

    public OpenMeteoClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    /**
     * 取当前天气 + 今日日出日落(一次请求)。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return Open-Meteo 响应(含 current 云量 + daily sunrise/sunset)
     */
    public OpenMeteoResponse getForecast(double lat, double lng) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lng)
                        .queryParam("current", "cloud_cover,temperature_2m,relative_humidity_2m,weather_code")
                        .queryParam("daily", "sunrise,sunset")
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .body(OpenMeteoResponse.class);
    }

    /**
     * 将 ISO 8601 字符串(如 "2026-07-07T04:53")转换为 unix 秒。
     *
     * <p>Open-Meteo 返回的时间已带本地时区信息(timezone=auto),
     * 这里按 UTC 解析后转 epoch 秒,前端用 new Date(ms) 会自动按浏览器时区显示。</p>
     *
     * @param iso ISO 8601 字符串
     * @return unix 秒,解析失败返回 0
     */
    public static long isoToEpochSecond(String iso) {
        if (iso == null || iso.isBlank()) {
            return 0;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(iso, ISO_FORMATTER);
            return ldt.toEpochSecond(ZoneOffset.UTC);
        } catch (Exception e) {
            return 0;
        }
    }
}
