package com.cityglow.config;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.NasaApodResponse;
import com.cityglow.domain.NeoResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 本地缓存配置。
 *
 * <p>定义 3 个独立 Cache Bean,分别服务于:</p>
 * <ul>
 *   <li>{@code apodCache} - NASA APOD 接口缓存,key=date 字符串,TTL 30 分钟</li>
 *   <li>{@code neoCache} - NASA NeoWs 近地小行星缓存,key="feed",TTL 1 小时</li>
 *   <li>{@code forecastCache} - 观星预报缓存,key="lat,lng",TTL 10 分钟</li>
 * </ul>
 *
 * <p>所有 Cache 用 {@link Caffeine#expireAfterWrite} 设置 TTL,
 * 写入后达到 TTL 自动失效,下次访问触发回源。</p>
 */
@Configuration
public class CacheConfig {

    /** APOD 缓存 TTL:30 分钟。 */
    private static final Duration APOD_TTL = Duration.ofMinutes(30);

    /** 近地小行星缓存 TTL:1 小时。 */
    private static final Duration NEO_TTL = Duration.ofHours(1);

    /** 观星预报缓存 TTL:10 分钟。 */
    private static final Duration FORECAST_TTL = Duration.ofMinutes(10);

    /**
     * NASA APOD 缓存:key=date 字符串(YYYY-MM-DD)。
     *
     * @return APOD 缓存实例
     */
    @Bean
    public Cache<String, NasaApodResponse> apodCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(APOD_TTL)
                .build();
    }

    /**
     * NASA NeoWs 近地小行星缓存:key="feed"(单条目)。
     *
     * @return 近地小行星缓存实例
     */
    @Bean
    public Cache<String, NeoResponse> neoCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(NEO_TTL)
                .build();
    }

    /**
     * 观星预报缓存:key="lat,lng"(保留 4 位小数)。
     *
     * @return 观星预报缓存实例
     */
    @Bean
    public Cache<String, ForecastResult> forecastCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(FORECAST_TTL)
                .build();
    }
}
