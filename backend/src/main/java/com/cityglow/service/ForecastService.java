package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenMeteoResponse;
import com.cityglow.util.BortleEstimator;
import com.cityglow.util.Messages;
import com.cityglow.util.MoonPhaseCalculator;
import com.cityglow.util.MoonPhaseDescription;
import com.cityglow.util.StargazingIndex;
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
 */
@Service
public class ForecastService {

    /** 湿度兜底值(百分比)。 */
    private static final double DEFAULT_HUMIDITY = 50.0;

    /** 温度兜底值(摄氏度)。 */
    private static final double DEFAULT_TEMPERATURE_C = 15.0;

    private final OpenMeteoClient openMeteoClient;

    public ForecastService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    /**
     * 计算给定经纬度的观星预报(默认中文)。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 观星预报结果
     * @throws RuntimeException 若 Open-Meteo 调用失败
     */
    public ForecastResult forecast(double lat, double lng) {
        return forecast(lat, lng, Messages.DEFAULT_LOCALE);
    }

    /**
     * 计算给定经纬度的观星预报,使用指定 Locale 输出多语言描述。
     *
     * @param lat    纬度
     * @param lng    经度
     * @param locale 语言(zh/en/ja,其他回退到 zh)
     * @return 观星预报结果(含 score/cloudCover/moonPhase/bortleLevel/bortleDescription/message/sunrise/sunset)
     * @throws RuntimeException 若 Open-Meteo 调用失败
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
}
