package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenMeteoResponse;
import com.cityglow.util.BortleEstimator;
import com.cityglow.util.MoonPhaseCalculator;
import com.cityglow.util.MoonPhaseDescription;
import com.cityglow.util.StargazingIndex;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 观星预报服务(设计文档第 4 节模块 2)。
 *
 * <p>核心职责:调用 <b>Open-Meteo</b>(免费开源,无需 API Key)
 * {@code /v1/forecast} 端点取云量和日出日落,用 {@link MoonPhaseCalculator}
 * 根据日期算月相,合并后调 {@link StargazingIndex} 计算观星指数,返回 {@link ForecastResult}。</p>
 *
 * <p><b>为什么用 Open-Meteo 而非 OpenWeatherMap</b>:OpenWeatherMap One Call 3.0
 * 需单独订阅(免费 key 调用返回 401),Open-Meteo 完全免费、无 key、无速率限制。</p>
 *
 * <p><b>兜底逻辑</b>:Open-Meteo 端点偶发返回 null(JSON 缺字段),
 * 对云量兜底 100(全阴),sunrise/sunset 兜底 0。</p>
 */
@Service
public class ForecastService {

    private final OpenMeteoClient openMeteoClient;

    public ForecastService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    /**
     * 计算给定经纬度的观星预报。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 观星预报结果(含 score/cloudCover/moonPhase/bortleLevel/message/sunrise/sunset)
     * @throws RuntimeException 若 Open-Meteo 调用失败
     */
    public ForecastResult forecast(double lat, double lng) {
        int bortleLevel = BortleEstimator.estimate(lat, lng);

        // 调用 Open-Meteo /v1/forecast(免费,无 key)
        OpenMeteoResponse response = openMeteoClient.getForecast(lat, lng);

        // 兜底:null 响应时云量按全阴 100
        double cloudCover = (response != null && response.current() != null)
                ? response.current().cloudCover() : 100;
        long sunrise = (response != null)
                ? OpenMeteoClient.isoToEpochSecond(response.getTodaySunrise()) : 0;
        long sunset = (response != null)
                ? OpenMeteoClient.isoToEpochSecond(response.getTodaySunset()) : 0;

        // 月相用天文算法计算(Open-Meteo 无月相字段)
        double moonPhase = MoonPhaseCalculator.calculatePhase(LocalDate.now());
        double moonIlluminatedFraction = MoonPhaseCalculator.toIlluminatedFraction(moonPhase);

        int score = StargazingIndex.calculate(cloudCover, moonIlluminatedFraction, bortleLevel);
        String message = StargazingIndex.getMessage(score);
        String moonPhaseDesc = MoonPhaseDescription.fromPhase(moonPhase);

        return new ForecastResult(
                score, cloudCover, moonPhaseDesc, bortleLevel, message, sunrise, sunset);
    }
}
