package com.cityglow.service;

import com.cityglow.domain.BestWindowResult;
import com.cityglow.domain.ForecastResult;
import com.cityglow.domain.OpenMeteoResponse;
import com.cityglow.util.MoonPhaseCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 今晚最佳观测时段推荐服务。
 *
 * <p>核心职责:综合云量、月相、月升月落、Bortle 暗空等级、日出日落,
 * 计算今晚最适合观星的时间窗口,并给出评分与原因列表。</p>
 *
 * <p><b>暗夜窗口</b>:从天文昏影终(日落 + ~1.5 小时)到天文晨光始(日出 - ~1.5 小时)。
 * 此窗口内太阳低于地平线 18° 以下,天空足够黑暗,可观测深空天体。</p>
 *
 * <p><b>月升月落估算</b>:用简化线性算法,基于月相相位(phase 0-1)与日出日落推导:
 * moonrise ≈ sunrise + 24h × phase,moonset ≈ sunset + 24h × phase(均 mod 24h)。
 * 这是粗略估算,实际月升月落受纬度、月赤纬影响,有 ±2 小时误差,
 * 但对"是否在观测窗口内"的判断已足够。</p>
 *
 * <p><b>评分</b>:以 ForecastService 的观星指数为基础分,叠加月亮在窗口内的影响:
 * 月落于窗口内(+10,天空变暗)、月升于窗口内(-10 × 月亮照亮比例,天空变亮)。
 * 云量 &gt; 60% 直接判定不适合观星。</p>
 *
 * <p><b>依赖</b>:注入 {@link ForecastService} 取已缓存的观星预报结果;
 * 注入 {@link OpenMeteoClient} 作为兜底,当 ForecastResult 中 sunrise/sunset 为 0
 * (Open-Meteo 偶发返回 null daily)时,直接调 OpenMeteoClient 取 ISO 字符串解析。</p>
 */
@Service
public class BestWindowService {

    /** 天文昏影终/晨光始偏移:日落/日出后/前 1.5 小时。 */
    private static final int ASTRONOMICAL_TWILIGHT_OFFSET_MINUTES = 90;

    /** 月落于窗口内的加分。 */
    private static final int MOONSET_BONUS = 10;

    /** 月升于窗口内的基础扣分(再乘月亮照亮比例)。 */
    private static final int MOONRISE_PENALTY_BASE = 10;

    /** 云量阈值:超过此值判定不适合观星。 */
    private static final double HIGH_CLOUD_THRESHOLD = 60.0;

    /** 一天的分钟数,用于 mod 计算。 */
    private static final int MINUTES_PER_DAY = 24 * 60;

    /** Open-Meteo 返回的 ISO 8601 本地时间格式(如 "2026-07-07T04:53")。 */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ForecastService forecastService;
    private final OpenMeteoClient openMeteoClient;

    public BestWindowService(ForecastService forecastService, OpenMeteoClient openMeteoClient) {
        this.forecastService = forecastService;
        this.openMeteoClient = openMeteoClient;
    }

    /**
     * 计算给定经纬度今晚的最佳观测时段。
     *
     * @param lat 纬度
     * @param lng 经度
     * @return BestWindowResult,含最佳时段、评分、原因列表
     */
    public BestWindowResult getBestWindow(double lat, double lng) {
        LocalDate today = LocalDate.now();
        List<String> reasons = new ArrayList<>();

        // 1. 调 ForecastService 取当前数据(已缓存)
        ForecastResult forecast = forecastService.forecast(lat, lng);

        double cloudCover = forecast.cloudCover();
        int bortleLevel = forecast.bortleLevel();
        long sunriseEpoch = forecast.sunrise();
        long sunsetEpoch = forecast.sunset();

        // 2. 计算暗夜窗口:start = 日落 + 1.5h(天文昏影终),end = 日出 - 1.5h(天文晨光始)
        LocalTime sunsetTime = resolveSunsetTime(lat, lng, sunsetEpoch);
        LocalTime sunriseTime = resolveSunriseTime(lat, lng, sunriseEpoch);
        LocalTime windowStart = plusMinutesWrap(sunsetTime, ASTRONOMICAL_TWILIGHT_OFFSET_MINUTES);
        LocalTime windowEnd = minusMinutesWrap(sunriseTime, ASTRONOMICAL_TWILIGHT_OFFSET_MINUTES);

        // 3. 月相 + 估算月升月落时间
        double moonPhase = MoonPhaseCalculator.calculatePhase(today);
        double moonIlluminated = MoonPhaseCalculator.toIlluminatedFraction(moonPhase);
        LocalTime moonrise = estimateMoonrise(sunriseTime, moonPhase);
        LocalTime moonset = estimateMoonset(sunsetTime, moonPhase);

        // 4. 评分:基础分来自 ForecastResult
        int score = forecast.score();

        // 云量过高直接判定不适合
        if (cloudCover > HIGH_CLOUD_THRESHOLD) {
            return new BestWindowResult(
                    today, windowStart, windowEnd, 0,
                    "今晚云量较高,不适合观星",
                    List.of(String.format("云量 %.0f%% 超过 60%% 阈值", cloudCover))
            );
        }

        // 月亮在窗口内的影响
        boolean moonriseInWindow = isInWindow(moonrise, windowStart, windowEnd);
        boolean moonsetInWindow = isInWindow(moonset, windowStart, windowEnd);

        if (moonsetInWindow) {
            // 月落后天空更暗,加分
            score += MOONSET_BONUS;
            reasons.add(String.format("月落于 %s", moonset));
        }
        if (moonriseInWindow) {
            // 月升天空变亮,按月亮照亮比例扣分(满月扣最多)
            int penalty = (int) Math.round(MOONRISE_PENALTY_BASE * moonIlluminated);
            score -= penalty;
            reasons.add(String.format("月升于 %s(照亮 %.0f%%)", moonrise, moonIlluminated * 100));
        }

        // 其他原因
        if (cloudCover < 30) {
            reasons.add(String.format("云量仅 %.0f%%", cloudCover));
        } else {
            reasons.add(String.format("云量 %.0f%%", cloudCover));
        }
        reasons.add("Bortle " + bortleLevel);
        if (moonIlluminated < 0.1) {
            reasons.add("月相接近新月");
        } else if (moonIlluminated > 0.9) {
            reasons.add("月相接近满月");
        }

        score = Math.max(0, Math.min(100, score));

        String message = buildMessage(score);
        return new BestWindowResult(today, windowStart, windowEnd, score, message, reasons);
    }

    /**
     * 解析日落 LocalTime:优先用 ForecastResult 的 epoch,为 0 时回退调 OpenMeteoClient。
     *
     * @param lat         纬度
     * @param lng         经度
     * @param sunsetEpoch ForecastResult 中的日落 epoch 秒(可能为 0)
     * @return 日落 LocalTime
     */
    private LocalTime resolveSunsetTime(double lat, double lng, long sunsetEpoch) {
        if (sunsetEpoch > 0) {
            return epochToLocalTime(sunsetEpoch);
        }
        // 兜底:直接调 OpenMeteoClient 取 ISO 字符串
        OpenMeteoResponse response = openMeteoClient.getForecast(lat, lng);
        return parseIsoToLocalTime(response != null ? response.getTodaySunset() : null);
    }

    /**
     * 解析日出 LocalTime:优先用 ForecastResult 的 epoch,为 0 时回退调 OpenMeteoClient。
     *
     * @param lat         纬度
     * @param lng         经度
     * @param sunriseEpoch ForecastResult 中的日出 epoch 秒(可能为 0)
     * @return 日出 LocalTime
     */
    private LocalTime resolveSunriseTime(double lat, double lng, long sunriseEpoch) {
        if (sunriseEpoch > 0) {
            return epochToLocalTime(sunriseEpoch);
        }
        OpenMeteoResponse response = openMeteoClient.getForecast(lat, lng);
        return parseIsoToLocalTime(response != null ? response.getTodaySunrise() : null);
    }

    /**
     * 解析 ISO 8601 字符串(如 "2026-07-07T04:53")为 LocalTime。
     *
     * @param iso ISO 字符串,可能为 null
     * @return LocalTime,解析失败返回 LocalTime.MIDNIGHT
     */
    private LocalTime parseIsoToLocalTime(String iso) {
        if (iso == null || iso.isBlank()) {
            return LocalTime.MIDNIGHT;
        }
        try {
            return LocalDateTime.parse(iso, ISO_FORMATTER).toLocalTime();
        } catch (Exception e) {
            return LocalTime.MIDNIGHT;
        }
    }

    /**
     * 把 Open-Meteo 的 epoch 秒(UTC 视角)转回 LocalTime。
     *
     * <p>OpenMeteoClient.isoToEpochSecond 把本地时间 ISO 字符串当 UTC 解析为 epoch,
     * 这里反向用 UTC 解码回 LocalDateTime,提取 LocalTime 即可还原原始本地时点。</p>
     *
     * @param epochSecond epoch 秒(0 表示无效,返回 00:00)
     * @return LocalTime
     */
    private LocalTime epochToLocalTime(long epochSecond) {
        if (epochSecond <= 0) {
            return LocalTime.MIDNIGHT;
        }
        return LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC).toLocalTime();
    }

    /**
     * LocalTime 加分钟(自动 mod 24h,跨日不报错)。
     */
    private LocalTime plusMinutesWrap(LocalTime t, int minutes) {
        long total = t.toSecondOfDay() + minutes * 60L;
        total = Math.floorMod(total, MINUTES_PER_DAY * 60L);
        return LocalTime.ofSecondOfDay(total);
    }

    /**
     * LocalTime 减分钟(自动 mod 24h,跨日不报错)。
     */
    private LocalTime minusMinutesWrap(LocalTime t, int minutes) {
        return plusMinutesWrap(t, -minutes);
    }

    /**
     * 估算月升时间:sunrise + 24h × phase(mod 24h)。
     *
     * <p>phase=0(新月):月升 ≈ 日出;phase=0.5(满月):月升 ≈ 日落;
     * phase=0.25(上弦):月升 ≈ 正午;phase=0.75(下弦):月升 ≈ 子夜。</p>
     *
     * @param sunrise 日出时间
     * @param phase   月相相位 0-1
     * @return 估算月升时间
     */
    private LocalTime estimateMoonrise(LocalTime sunrise, double phase) {
        long sunriseMinutes = sunrise.toSecondOfDay() / 60L;
        long moonriseMinutes = (long) (sunriseMinutes + MINUTES_PER_DAY * phase);
        moonriseMinutes = Math.floorMod(moonriseMinutes, MINUTES_PER_DAY);
        return LocalTime.ofSecondOfDay(moonriseMinutes * 60L);
    }

    /**
     * 估算月落时间:sunset + 24h × phase(mod 24h)。
     *
     * <p>phase=0(新月):月落 ≈ 日落;phase=0.5(满月):月落 ≈ 日出;
     * phase=0.25(上弦):月落 ≈ 子夜;phase=0.75(下弦):月落 ≈ 正午。</p>
     *
     * @param sunset 日落时间
     * @param phase  月相相位 0-1
     * @return 估算月落时间
     */
    private LocalTime estimateMoonset(LocalTime sunset, double phase) {
        long sunsetMinutes = sunset.toSecondOfDay() / 60L;
        long moonsetMinutes = (long) (sunsetMinutes + MINUTES_PER_DAY * phase);
        moonsetMinutes = Math.floorMod(moonsetMinutes, MINUTES_PER_DAY);
        return LocalTime.ofSecondOfDay(moonsetMinutes * 60L);
    }

    /**
     * 判断给定时间是否落在观测窗口内(支持跨午夜)。
     *
     * <p>窗口 21:15 → 03:23 跨午夜;12:00 → 14:00 不跨午夜。</p>
     *
     * @param t      待判断时间
     * @param start  窗口开始
     * @param end    窗口结束
     * @return true 表示在窗口内
     */
    private boolean isInWindow(LocalTime t, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            // 不跨午夜:start < t < end
            return !t.isBefore(start) && !t.isAfter(end);
        }
        // 跨午夜:t >= start OR t <= end
        return !t.isBefore(start) || !t.isAfter(end);
    }

    /**
     * 根据评分生成人类可读消息。
     *
     * @param score 评分 0-100
     * @return 消息字符串
     */
    private String buildMessage(int score) {
        if (score >= 80) {
            return "今夜极佳,适合通宵观测!";
        } else if (score >= 60) {
            return "今夜观测条件良好";
        } else if (score >= 40) {
            return "今夜可观测,但条件一般";
        } else if (score >= 20) {
            return "今夜观测条件较差";
        } else {
            return "今夜不适合观星";
        }
    }
}
