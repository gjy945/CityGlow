package com.cityglow.service;

import com.cityglow.domain.NasaApodResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * NASA APOD (Astronomy Picture of the Day) API 客户端(设计文档第 4 节模块 3)。
 *
 * <p>封装 {@code GET https://api.nasa.gov/planetary/apod},用 Spring 6.1 RestClient
 * 反序列化到 {@link NasaApodResponse} record。API Key 从
 * {@code application.yml} 的 {@code nasa.apod.key} 注入,严禁硬编码。</p>
 *
 * <p>NASA APOD 默认返回当日图片;传 {@code date} 参数(YYYY-MM-DD)可查历史。</p>
 */
@Component
public class NasaApodClient {

    private static final String BASE_URL = "https://api.nasa.gov/planetary/apod";

    private final RestClient restClient;
    private final String apiKey;

    public NasaApodClient(
            RestClient.Builder restClientBuilder,
            @Value("${nasa.apod.key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
    }

    /**
     * 取今日 APOD。
     *
     * @return APOD 响应(含标题、解释、图片 URL 等)
     */
    public NasaApodResponse getApod() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(NasaApodResponse.class);
    }

    /**
     * 取指定日期的 APOD。
     *
     * @param date 日期字符串,格式 YYYY-MM-DD
     * @return APOD 响应
     */
    public NasaApodResponse getApod(String date) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("api_key", apiKey)
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .body(NasaApodResponse.class);
    }
}
