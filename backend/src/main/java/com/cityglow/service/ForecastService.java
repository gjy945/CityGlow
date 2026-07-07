package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenMeteoResponse;
import com.cityglow.util.BortleEstimator;
import com.cityglow.util.Messages;
import com.cityglow.util.MoonPhaseCalculator;
import com.cityglow.util.MoonPhaseDescription;
import com.cityglow.util.StargazingIndex;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;

/**
 * 观星预报服务(设计文档第 4 节模块 2)。
 *
 * <p>核心职责:调用 <b>Open-Meteo</b>(免费开源,无需 API Key)
 * {@code /v1/forecast} 端点取云量、湿度、温度和日出日落,用 {@link MoonPhaseCalculator}
 * 根据日期算月相,合并后调 {@link StargazingIndex} 计算观星指数,返回 {@link ForecastResult}。</p>
 *
 * <p><b>为什么用 Open-Meteo 而非 OpenWeatherMap</b>:OpenWeatherMap One Call 3.0
 * 需单独订阅(免费 key 调用返回 401),Open-Meteo 完全免费、无 key、无速率限制。</p>
 *
 * <p><b>兜底逻辑</b>:Open-Meteo 端点偶发返回 null(JSON 缺字段),
 * 对云量兜底 100(全阴),sunrise/sunset 兜底 0,湿度兜底 50,温度兜底 15。</p>
 *
 * <p><b>多语言</b>:Locale 由 Controller 解析 Accept-Language 后传入,
 * 默认 zh(见 {@link Messages#DEFAULT_LOCALE})。</p>
 *
 * <p><b>缓存</b>:{@link #forecast(double, double)} 注入 {@code forecastCache},
 * key 格式 {@code lat,lng}(保留 4 位小数),TTL 10 分钟。
 * 命中直接返回,miss 时计算并写入缓存。
 * 含 Locale 参数的重载方法不参与缓存,以便不同语言请求都能拿到对应描述。</p>
 */
@Service
public class ForecastService {

    /** 湿度兜底值(百分比)。 */
    private static final double DEFAULT_HUMIDITY = 50.0;

    /** 温度兜底值(摄氏度)。 */
    private static final double DEFAULT_TEMPERATURE_C = 15.0;

    /** 缓存 key 经纬度保留小数位数。 */
    private static final int KEY_SCALE = 4;

    private final OpenMeteoClient openMeteoClient;
    private final Cache<String, ForecastResult> forecastCache;

    public ForecastService(
            OpenMeteoClient openMeteoClient,
            @Qualifier("forecastCache") Cache<String, ForecastResult> forecastCache) {
        this.openMeteoClient = openMeteoClient;
        this.forecastCache = forecastCache;
    }

    /**
     * 计算给定经纬度的观星预报(默认中文),先查缓存。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 观星预报结果
     */
    public ForecastResult forecast(double lat, double lng) {
        String key = formatKey(lat, lng);
        // 缓存命中直接返回;miss 时调用 forecast(lat, lng, default locale) 并写入缓存
        return forecastCache.get(key, k -> forecast(lat, lng, Messages.DEFAULT_LOCALE));
    }

    /**
     * 计算给定经纬度的观星预报,使用指定 Locale 输出多语言描述(不查缓存)。
     *
     * @param lat    纬度
     * @param lng    经度
     * @param locale 语言(zh/en/ja,其他回退到 zh)
     * @return 观星预报结果(含 score/cloudCover/moonPhase/bortleLevel/bortleDescription/message/sunrise/sunset)
     */
    public ForecastResult forecast(double lat, double lng, Locale locale) {
        int bortleLevel = BortleEstimator.estimate(lat, lng);

        // 调用 Open-Meteo /v1/forecast(免费,无 key)
        OpenMeteoResponse response = openMeteoClient.getForecast(lat, lng);

        // 兜底:null 响应时云量按全阴 100
        double cloudCover = (response != null && response.current() != null)
                ? response.current().cloudCover() : 100;
        double humidity = (response != null && response.current() != null)
                ? response.current().relativeHumidity2m() : DEFAULT_HUMIDITY;
        double temperatureC = (response != null && response.current() != null)
                ? response.current().temperature2m() : DEFAULT_TEMPERATURE_C;
        long sunrise = (response != null)
                ? OpenMeteoClient.isoToEpochSecond(response.getTodaySunrise()) : 0;
        long sunset = (response != null)
                ? OpenMeteoClient.isoToEpochSecond(response.getTodaySunset()) : 0;

        // 月相用天文算法计算(Open-Meteo 无月相字段)
        double moonPhase = MoonPhaseCalculator.calculatePhase(LocalDate.now());
        double moonIlluminatedFraction = MoonPhaseCalculator.toIlluminatedFraction(moonPhase);

        int score = StargazingIndex.calculate(cloudCover, moonIlluminatedFraction,
                bortleLevel, humidity, temperatureC);
        String message = StargazingIndex.getMessage(score, locale);
        String moonPhaseDesc = MoonPhaseDescription.fromPhase(moonPhase, locale);
        String bortleDesc = BortleEstimator.getDescription(bortleLevel, locale);

        return new ForecastResult(
                score, cloudCover, moonPhaseDesc, bortleLevel, bortleDesc,
                message, sunrise, sunset);
    }

    /**
     * 构造缓存 key:格式 {@code lat,lng},保留 4 位小数。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 缓存 key 字符串
     */
    private String formatKey(double lat, double lng) {
        return String.format(Locale.ROOT, "%." + KEY_SCALE + "f,%.4f", lat, lng);
    }
}
