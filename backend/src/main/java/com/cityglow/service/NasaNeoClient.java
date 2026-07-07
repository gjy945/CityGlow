package com.cityglow.service;

import com.cityglow.domain.NeoResponse;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * NASA NeoWs (Near Earth Object Web Service) API 客户端。
 *
 * <p>封装 {@code GET https://api.nasa.gov/neo/rest/v1/feed} 端点,
 * 按日期范围查询接近地球的小行星列表。</p>
 *
 * <p><b>API 限制</b>:NeoWs /feed 端点的日期范围最多 7 天(含起止)。</p>
 *
 * <p><b>默认行为</b>:CityGlow 查询未来 7 天(today → today+7),
 * 让用户知道接下来一周哪些小行星会接近地球。
 * NASA 默认(无参数)只查当天,不适合"未来预报"场景。</p>
 *
 * <p><b>缓存</b>:注入 {@code neoCache},key="feed"(单条目),TTL 1 小时。</p>
 */
@Component
public class NasaNeoClient {

    private static final String BASE_URL = "https://api.nasa.gov/neo/rest/v1";

    /** NeoWs /feed 端点日期范围上限(含起止共 7 天)。 */
    private static final int MAX_DAYS = 7;

    /** 默认查未来 7 天。 */
    private static final int DEFAULT_DAYS = 7;

    /** 缓存 key(单条目,所有 days 参数共享同一缓存项)。 */
    private static final String CACHE_KEY = "feed";

    /** ISO 日期格式化器(YYYY-MM-DD)。 */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient restClient;
    private final String apiKey;
    private final Cache<String, NeoResponse> neoCache;

    public NasaNeoClient(
            RestClient.Builder restClientBuilder,
            @Value("${nasa.apod.key}") String apiKey,
            @Qualifier("neoCache") Cache<String, NeoResponse> neoCache) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
        this.neoCache = neoCache;
    }

    /**
     * 取未来 days 天接近地球的小行星(默认 7 天),先查缓存。
     *
     * <p>日期范围:startDate=today,endDate=today+days。NASA 限制 days ≤ 7。</p>
     *
     * @param days 查询未来天数(1-7,超出自动 clamp 到 7)
     * @return NeoResponse,按日期分组的近地小行星列表
     */
    public NeoResponse getFeed(int days) {
        int safeDays = clampDays(days);
        // 缓存命中直接返回;miss 时调 API 并写入缓存
        return neoCache.get(CACHE_KEY, key -> fetchFeed(safeDays));
    }

    /**
     * 取默认(未来 7 天)的近地小行星。
     *
     * @return NeoResponse
     */
    public NeoResponse getFeed() {
        return getFeed(DEFAULT_DAYS);
    }

    /**
     * 取指定日期范围的近地小行星(不走缓存,供测试与精细查询使用)。
     *
     * @param startDate 开始日期(含)
     * @param endDate   结束日期(含,NASA 限制 startDate 到 endDate 最多 7 天)
     * @return NeoResponse
     */
    public NeoResponse getFeed(LocalDate startDate, LocalDate endDate) {
        return fetchFeed(startDate, endDate);
    }

    /**
     * 调 /feed 端点取未来 days 天数据(缓存 miss 时触发)。
     *
     * @param days 未来天数(已 clamp 到 1-7)
     * @return NeoResponse
     */
    private NeoResponse fetchFeed(int days) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        return fetchFeed(today, end);
    }

    /**
     * 实际调 /feed 端点。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return NeoResponse
     */
    private NeoResponse fetchFeed(LocalDate startDate, LocalDate endDate) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/feed")
                        .queryParam("start_date", startDate.format(ISO_DATE))
                        .queryParam("end_date", endDate.format(ISO_DATE))
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(NeoResponse.class);
    }

    /**
     * Clamp days 到 [1, 7] 范围。
     *
     * @param days 输入天数
     * @return clamp 后的天数
     */
    private int clampDays(int days) {
        if (days < 1) {
            return 1;
        }
        return Math.min(days, MAX_DAYS);
    }
}
