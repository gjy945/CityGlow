package com.cityglow.service;

import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenWeatherCurrentResponse;
import com.cityglow.util.BortleEstimator;
import com.cityglow.util.MoonPhaseCalculator;
import com.cityglow.util.MoonPhaseDescription;
import com.cityglow.util.StargazingIndex;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 观星预报服务(设计文档第 4 节模块 2)。
 *
 * <p>核心职责:调用 OpenWeatherMap {@code /data/2.5/weather} 端点取云量和日出日落,
 * 用 {@link MoonPhaseCalculator} 根据日期算月相(不依赖 One Call API 3.0 订阅),
 * 合并后调 {@link StargazingIndex} 计算观星指数,返回 {@link ForecastResult}。</p>
 *
 * <p><b>为什么不再并行调两个端点</b>:OpenWeather One Call 3.0 需单独订阅,
 * 免费 key 调用会返回 401。改用免费版 {@code /weather} 端点(已含 sunrise/sunset),
 * 月相用天文算法计算,精度足够课程设计使用。</p>
 *
 * <p><b>兜底逻辑</b>:OpenWeather 端点偶发返回 null(JSON 缺字段),
 * 对云量兜底 100(全阴),sunrise/sunset 兜底 0。</p>
 */
@Service
public class ForecastService {

    private final OpenWeatherClient openWeatherClient;

    public ForecastService(OpenWeatherClient openWeatherClient) {
        this.openWeatherClient = openWeatherClient;
    }

    /**
     * 计算给定经纬度的观星预报。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return 观星预报结果(含 score/cloudCover/moonPhase/bortleLevel/message/sunrise/sunset)
     * @throws RuntimeException 若 OpenWeather 调用失败
     */
    public ForecastResult forecast(double lat, double lng) {
        int bortleLevel = BortleEstimator.estimate(lat, lng);

        // 调用 /weather 端点(免费 key 支持)
        OpenWeatherCurrentResponse current = openWeatherClient.getCurrentWeather(lat, lng);

        // 兜底:null 响应时云量按全阴 100
        double cloudCover = current != null ? current.getCloudCover() : 100;
        long sunrise = current != null ? current.getSunrise() : 0;
        long sunset = current != null ? current.getSunset() : 0;

        // 月相用天文算法计算(不依赖 OpenWeather One Call API)
        double moonPhase = MoonPhaseCalculator.calculatePhase(LocalDate.now());
        double moonIlluminatedFraction = MoonPhaseCalculator.toIlluminatedFraction(moonPhase);

        int score = StargazingIndex.calculate(cloudCover, moonIlluminatedFraction, bortleLevel);
        String message = StargazingIndex.getMessage(score);
        String moonPhaseDesc = MoonPhaseDescription.fromPhase(moonPhase);

        return new ForecastResult(
                score, cloudCover, moonPhaseDesc, bortleLevel, message, sunrise, sunset);
    }
}
