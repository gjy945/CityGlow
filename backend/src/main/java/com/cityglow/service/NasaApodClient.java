package com.cityglow.service;

import com.cityglow.domain.NasaApodResponse;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * NASA APOD (Astronomy Picture of the Day) API 客户端(设计文档第 4 节模块 3)。
 *
 * <p>封装 {@code GET https://api.nasa.gov/planetary/apod},用 Spring 6.1 RestClient
 * 反序列化到 {@link NasaApodResponse} record。API Key 从
 * {@code application.yml} 的 {@code nasa.apod.key} 注入,严禁硬编码。</p>
 *
 * <p>NASA APOD 默认返回当日图片;传 {@code date} 参数(YYYY-MM-DD)可查历史。</p>
 *
 * <p><b>缓存</b>:注入 {@link Cache}({@code apodCache}),key=date 字符串,
 * 命中则直接返回,miss 才调 NASA API 并写入缓存。TTL 由 CacheConfig 配置(30 分钟)。</p>
 */
@Component
public class NasaApodClient {

    private static final String BASE_URL = "https://api.nasa.gov/planetary/apod";

    /** ISO 日期格式化器(YYYY-MM-DD)。 */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient restClient;
    private final String apiKey;
    private final Cache<String, NasaApodResponse> apodCache;

    public NasaApodClient(
            RestClient.Builder restClientBuilder,
            @Value("${nasa.apod.key}") String apiKey,
            @Qualifier("apodCache") Cache<String, NasaApodResponse> apodCache) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
        this.apodCache = apodCache;
    }

    /**
     * 取今日 APOD(使用今天日期作为缓存 key)。
     *
     * @return APOD 响应(含标题、解释、图片 URL 等)
     */
    public NasaApodResponse getApod() {
        return getApod(LocalDate.now().format(ISO_DATE));
    }

    /**
     * 取指定日期的 APOD,先查缓存,miss 再调 API 并存缓存。
     *
     * @param date 日期字符串,格式 YYYY-MM-DD
     * @return APOD 响应
     */
    public NasaApodResponse getApod(String date) {
        // 缓存命中直接返回;miss 时调用 NASA API 并写入缓存
        return apodCache.get(date, key -> fetchApodFromApi(key));
    }

    /**
     * 实际调用 NASA APOD API(缓存 miss 时触发)。
     *
     * <p>NASA 按 UTC 时间每日更新,有时当天数据尚未生成(返回 404)。
     * 此方法在 404 时自动回退到前一天重试,最多回退 3 天,
     * 全部失败则抛 RuntimeException 由上层处理。</p>
     *
     * @param date 日期字符串 YYYY-MM-DD
     * @return APOD 响应
     */
    private NasaApodResponse fetchApodFromApi(String date) {
        LocalDate current = LocalDate.parse(date, ISO_DATE);
        // 最多回退 3 天:NASA 偶发当日数据未生成(UTC 时差 / 节假日延迟)
        for (int i = 0; i <= 3; i++) {
            String tryDate = current.minusDays(i).format(ISO_DATE);
            try {
                return restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("api_key", apiKey)
                                .queryParam("date", tryDate)
                                .build())
                        .retrieve()
                        .body(NasaApodResponse.class);
            } catch (HttpClientErrorException.NotFound e) {
                // 当天数据未生成,继续回退到前一天
            }
        }
        throw new RuntimeException("NASA APOD 连续 4 天均无数据,可能是 API 故障");
    }
}
