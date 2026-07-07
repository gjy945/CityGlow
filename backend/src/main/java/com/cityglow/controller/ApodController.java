package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.domain.NasaApodResponse;
import com.cityglow.service.NasaApodClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NASA APOD REST 接口(设计文档第 4 节模块 3)。
 *
 * <p>暴露 {@code GET /api/v1/apod},返回 NASA 每日天文一图。
 * 内置 24h 内存缓存:同一日内多次请求只调一次 NASA API,降本提速。</p>
 *
 * <p>缓存实现:两个 {@code volatile} 字段({@link #cached} + {@link #cacheTimestamp})
 * + 双读,线程安全够用(最坏情况下并发首请求会触发多次回源,结果一致,无正确性问题)。
 * 不引入 Spring Cache 抽象(YAGNI:单端点、单条目,无需抽象)。</p>
 */
@RestController
@RequestMapping("/api/v1/apod")
public class ApodController {

    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24h

    private final NasaApodClient apodClient;

    /** 缓存的 APOD 响应,volatile 保证多线程可见性。null 表示未缓存。 */
    private volatile NasaApodResponse cached;

    /** 缓存写入时间戳(毫秒),volatile 保证多线程可见性。 */
    private volatile long cacheTimestamp;

    public ApodController(NasaApodClient apodClient) {
        this.apodClient = apodClient;
    }

    /**
     * 取今日 APOD,24h 内复用缓存。
     *
     * @return 统一 ApiResponse 包装的 APOD 响应
     */
    @GetMapping
    public ApiResponse<NasaApodResponse> getApod() {
        long now = System.currentTimeMillis();
        NasaApodResponse current = cached;
        if (current != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return ApiResponse.success(current);
        }
        NasaApodResponse fresh = apodClient.getApod();
        cached = fresh;
        cacheTimestamp = now;
        return ApiResponse.success(fresh);
    }

    /**
     * 清空缓存(主要用于测试)。包级可见,外部不暴露。
     */
    void clearCache() {
        cached = null;
        cacheTimestamp = 0L;
    }
}
