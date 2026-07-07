package com.cityglow.service;

import com.cityglow.domain.StarPoint;
import com.cityglow.service.ConstellationDataService.StarRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 星空投影服务 — 把赤道坐标(RA/Dec)转换为地平坐标(Az/Alt)。
 *
 * <p>算法步骤:</p>
 * <ol>
 *   <li>计算儒略日 JD(基于观测者当地日期+小时,转 UTC)</li>
 *   <li>计算格林尼治平恒星时 GMST</li>
 *   <li>计算当地恒星时 LST = GMST + 经度</li>
 *   <li>对每颗星:时角 H = LST - RA → 高度角 Alt + 方位角 Az</li>
 *   <li>过滤掉 Alt ≤ 0 的星(地平线以下不可见)</li>
 * </ol>
 *
 * <p><b>时区假设</b>:前端传入的 date+hour 是 Asia/Shanghai 时区的当地时间。
 * 这里转 UTC 后计算 GMST。如果用户在其他时区,前端可以预先转 UTC,
 * 但 MVP 假设中国境内观测点,统一用 Asia/Shanghai。</p>
 *
 * <p><b>坐标约定</b>:</p>
 * <ul>
 *   <li>方位角 Az:0=北,90=东,180=南,270=西(从北向东顺时针)</li>
 *   <li>高度角 Alt:0=地平线,90=天顶</li>
 *   <li>赤经 RA:0-360 度</li>
 *   <li>赤纬 Dec:-90 到 +90 度</li>
 * </ul>
 */
@Service
public class StarProjectionService {

    /** J2000 历元参考儒略日。 */
    private static final double J2000 = 2451545.0;

    /**
     * 把一组星(赤道坐标)投影到观测者当地的地平坐标。
     *
     * @param latitude  观测者纬度(度)
     * @param longitude 观测者经度(度,东经为正)
     * @param date      当地日期
     * @param hour      当地小时(0-23)
     * @param stars     待投影的星列表(赤道坐标)
     * @return 地平线以上的星列表(已过滤)
     */
    public List<StarPoint> project(
            double latitude,
            double longitude,
            LocalDate date,
            int hour,
            List<StarRecord> stars) {

        double jd = toJulianDate(date, hour);
        double gmst = calculateGmst(jd);
        double lst = normalizeAngle(gmst + longitude);
        double latRad = Math.toRadians(latitude);

        List<StarPoint> visible = new ArrayList<>();
        for (StarRecord star : stars) {
            double[] azAlt = equatorialToHorizontal(star.ra(), star.dec(), lst, latRad);
            double az = azAlt[0];
            double alt = azAlt[1];
            if (alt > 0) {
                visible.add(new StarPoint(star.hip(), star.mag(), az, alt));
            }
        }
        return visible;
    }

    /**
     * LocalDate + hour → 儒略日。
     * 假设输入是 Asia/Shanghai 当地时间,转 UTC 后算 JD。
     */
    private double toJulianDate(LocalDate date, int hour) {
        LocalDateTime local = date.atTime(hour, 0);
        ZonedDateTime utc = local.atZone(ZoneId.of("Asia/Shanghai"))
                .withZoneSameInstant(ZoneOffset.UTC);
        long epochDay = utc.toLocalDate().toEpochDay();
        return 2440587.5 + epochDay
                + utc.getHour() / 24.0
                + utc.getMinute() / 1440.0
                + utc.getSecond() / 86400.0;
    }

    /**
     * 计算格林尼治平恒星时(度)。
     */
    private double calculateGmst(double jd) {
        double t = (jd - J2000) / 36525.0;
        double gmst = 280.46061837
                + 360.98564736629 * (jd - J2000)
                + 0.000387933 * t * t
                - t * t * t / 38710000.0;
        return normalizeAngle(gmst);
    }

    /**
     * 赤道坐标 → 地平坐标。
     *
     * @param ra    赤经(度)
     * @param dec   赤纬(度)
     * @param lst   当地恒星时(度)
     * @param latRad 观测者纬度(弧度)
     * @return [azimuth, altitude] 度数
     */
    private double[] equatorialToHorizontal(double ra, double dec, double lst, double latRad) {
        double haRad = Math.toRadians(normalizeAngle(lst - ra));
        double decRad = Math.toRadians(dec);

        double sinAlt = Math.sin(decRad) * Math.sin(latRad)
                      + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        sinAlt = Math.max(-1, Math.min(1, sinAlt));
        double alt = Math.toDegrees(Math.asin(sinAlt));

        double cosAlt = Math.cos(Math.toRadians(alt));
        double cosAz = 1.0;
        if (Math.abs(cosAlt) > 1e-10) {
            double numerator = Math.sin(decRad) - Math.sin(Math.toRadians(alt)) * Math.sin(latRad);
            double denom = cosAlt * Math.cos(latRad);
            if (Math.abs(denom) > 1e-10) {
                cosAz = numerator / denom;
                cosAz = Math.max(-1, Math.min(1, cosAz));
            }
        }
        double az = Math.toDegrees(Math.acos(cosAz));
        if (Math.sin(haRad) > 0) {
            az = 360 - az;
        }
        return new double[]{normalizeAngle(az), alt};
    }

    /** 角度规范化到 [0, 360)。 */
    private double normalizeAngle(double deg) {
        double r = deg % 360;
        if (r < 0) r += 360;
        return r;
    }
}
