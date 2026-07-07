package com.cityglow.service;

import com.cityglow.domain.DonkiResponse;
import com.cityglow.domain.DonkiResponse.GeomagneticStorm;
import com.cityglow.domain.DonkiResponse.SolarFlare;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * NASA DONKI (Database Of Notifications, Knowledge, and Information) API 客户端。
 *
 * <p>封装 NASA DONKI 两个端点,用于极光预报:</p>
 * <ul>
 *   <li>{@code /GST} - 地磁暴(Geomagnetic Storm)事件,可能引发极光</li>
 *   <li>{@code /FLR} - 太阳耀斑(Solar Flare)事件,X 级耀斑常引发强地磁暴</li>
 * </ul>
 *
 * <p><b>并行调用</b>:用虚拟线程 {@link Executors#newVirtualThreadPerTaskExecutor()}
 * 同时调两个端点,降低延迟。JDK 21 虚拟线程轻量,数千并发无压力。</p>
 *
 * <p><b>缓存</b>:注入 {@code spaceWeatherCache},key="aurora"(单条目),TTL 30 分钟。
 * 命中直接返回,miss 时并行调两个端点并合并结果。</p>
 *
 * <p><b>默认时间范围</b>:地磁暴查近 30 天(可配),耀斑固定查近 7 天
 * (X 级以上才会引发极光,7 天已足够覆盖近期可能引发极光的耀斑)。</p>
 */
@Component
public class NasaDonkiClient {

    private static final String BASE_URL = "https://api.nasa.gov/DONKI";

    /** ISO 日期格式化器(YYYY-MM-DD)。 */
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 默认查询近 30 天的地磁暴。 */
    private static final int DEFAULT_GST_DAYS = 30;

    /** 耀斑固定查近 7 天(X 级耀斑影响极光的窗口期)。 */
    private static final int FLARE_DAYS = 7;

    /** 缓存 key(单条目,所有 days 参数共享同一缓存项)。 */
    private static final String CACHE_KEY = "aurora";

    private final RestClient restClient;
    private final String apiKey;
    private final Cache<String, DonkiResponse> spaceWeatherCache;

    /**
     * 虚拟线程执行器:用于并行调 GST 与 FLR 端点。
     *
     * <p>注:执行器生命周期与 bean 相同,无需显式关闭(JVM 退出时回收)。</p>
     */
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public NasaDonkiClient(
            RestClient.Builder restClientBuilder,
            @Value("${nasa.apod.key}") String apiKey,
            @Qualifier("spaceWeatherCache") Cache<String, DonkiResponse> spaceWeatherCache) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
        this.spaceWeatherCache = spaceWeatherCache;
    }

    /**
     * 取近 days 天的极光预报(默认 30 天),先查缓存,miss 再并行调两个端点。
     *
     * @param days 查询地磁暴的天数(耀斑固定 7 天)
     * @return 合并的 DONKI 响应
     */
    public DonkiResponse getAuroraForecast(int days) {
        // 缓存命中直接返回;miss 时并行调两个端点并写入缓存
        return spaceWeatherCache.get(CACHE_KEY, key -> fetchAuroraForecast(days));
    }

    /**
     * 取默认(30 天)极光预报。
     *
     * @return DONKI 响应
     */
    public DonkiResponse getAuroraForecast() {
        return getAuroraForecast(DEFAULT_GST_DAYS);
    }

    /**
     * 并行调 GST + FLR 端点并合并结果(缓存 miss 时触发)。
     *
     * @param days 地磁暴查询天数
     * @return 合并的 DONKI 响应
     */
    private DonkiResponse fetchAuroraForecast(int days) {
        LocalDate today = LocalDate.now();
        LocalDate gstStart = today.minusDays(days);
        LocalDate flrStart = today.minusDays(FLARE_DAYS);

        // 用虚拟线程并行调两个端点
        Future<List<GeomagneticStorm>> gstFuture = virtualThreadExecutor.submit(
                () -> fetchGstEvents(gstStart, today));
        Future<List<SolarFlare>> flrFuture = virtualThreadExecutor.submit(
                () -> fetchFlrEvents(flrStart, today));

        try {
            List<GeomagneticStorm> gstEvents = gstFuture.get();
            List<SolarFlare> flrEvents = flrFuture.get();
            return new DonkiResponse(gstEvents, flrEvents);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DONKI parallel fetch interrupted", e);
        } catch (ExecutionException e) {
            // 解包执行异常,保留原始 cause
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            throw new RuntimeException("Failed to fetch DONKI aurora forecast", cause);
        }
    }

    /**
     * 调 /GST 端点取地磁暴事件列表。
     *
     * @param startDate 开始日期(含)
     * @param endDate   结束日期(含)
     * @return 地磁暴事件列表(可能为空)
     */
    private List<GeomagneticStorm> fetchGstEvents(LocalDate startDate, LocalDate endDate) {
        GeomagneticStorm[] events = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/GST")
                        .queryParam("startDate", startDate.format(ISO_DATE))
                        .queryParam("endDate", endDate.format(ISO_DATE))
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(GeomagneticStorm[].class);
        return (events == null) ? List.of() : List.of(events);
    }

    /**
     * 调 /FLR 端点取太阳耀斑事件列表。
     *
     * @param startDate 开始日期(含)
     * @param endDate   结束日期(含)
     * @return 太阳耀斑事件列表(可能为空)
     */
    private List<SolarFlare> fetchFlrEvents(LocalDate startDate, LocalDate endDate) {
        SolarFlare[] events = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/FLR")
                        .queryParam("startDate", startDate.format(ISO_DATE))
                        .queryParam("endDate", endDate.format(ISO_DATE))
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(SolarFlare[].class);
        return (events == null) ? List.of() : List.of(events);
    }

    /**
     * 暴露给测试用的内部方法:直接调 /GST 端点(用于单元测试)。
     *
     * <p>包级可见,仅用于 {@link com.cityglow.service.NasaDonkiClientTest} 验证 URL 与字段映射。</p>
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 地磁暴事件列表
     */
    List<GeomagneticStorm> fetchGstEventsForTest(LocalDate startDate, LocalDate endDate) {
        return fetchGstEvents(startDate, endDate);
    }

    /**
     * 暴露给测试用的内部方法:直接调 /FLR 端点(用于单元测试)。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 太阳耀斑事件列表
     */
    List<SolarFlare> fetchFlrEventsForTest(LocalDate startDate, LocalDate endDate) {
        return fetchFlrEvents(startDate, endDate);
    }

    /**
     * 暴露给测试用的内部方法:直接调 fetchAuroraForecast(用于单元测试)。
     *
     * @param days 地磁暴查询天数
     * @return 合并的 DONKI 响应
     */
    DonkiResponse fetchAuroraForecastForTest(int days) {
        return fetchAuroraForecast(days);
    }
}
