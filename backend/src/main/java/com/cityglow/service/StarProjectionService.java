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

        // ═══════ 第 1 步:把当地日期+小时转成儒略日 JD ═══════
        // 儒略日是从公元前 4713 年开始的连续天数,天文学用它来方便地计算时间间隔
        double jd = toJulianDate(date, hour);

        // ═══════ 第 2 步:由 JD 算格林尼治平恒星时 GMST ═══════
        // GMST 表示格林尼治子午圈相对春分点的角度,反映地球的自转状态
        double gmst = calculateGmst(jd);

        // ═══════ 第 3 步:算当地恒星时 LST = GMST + 经度 ═══════
        // 东经为正,加上经度后得到观测者所在子午线的恒星时
        // LST 用来计算时角 H = LST - RA,即天体相对当地子午圈的角度
        double lst = normalizeAngle(gmst + longitude);

        // 纬度转弧度(三角函数要用弧度)
        double latRad = Math.toRadians(latitude);

        List<StarPoint> visible = new ArrayList<>();
        // ═══════ 第 4 步:逐颗星做坐标转换 ═══════
        for (StarRecord star : stars) {
            // 调用球面三角公式,把 (RA, Dec) 转成 (Az, Alt)
            double[] azAlt = equatorialToHorizontal(star.ra(), star.dec(), lst, latRad);
            double az = azAlt[0];
            double alt = azAlt[1];
            // ═══════ 第 5 步:过滤地平线以下的星 ═══════
            // 高度角 Alt ≤ 0 表示星在地平线以下,观测者看不到,直接丢弃
            if (alt > 0) {
                visible.add(new StarPoint(star.hip(), star.mag(), az, alt));
            }
        }
        return visible;
    }

    /**
     * LocalDate + hour → 儒略日。
     *
     * <p>假设输入是 Asia/Shanghai 当地时间,转 UTC 后算 JD。
     * 公式:JD = 2440587.5 + epochDay + hour/24 + minute/1440 + second/86400
     * (2440587.5 是 1970-01-01T00:00:00Z 对应的儒略日)</p>
     */
    private double toJulianDate(LocalDate date, int hour) {
        LocalDateTime local = date.atTime(hour, 0);
        // Asia/Shanghai 当地时间 → UTC,保证 GMST 计算基准统一
        ZonedDateTime utc = local.atZone(ZoneId.of("Asia/Shanghai"))
                .withZoneSameInstant(ZoneOffset.UTC);
        long epochDay = utc.toLocalDate().toEpochDay();
        // 2440587.5 = Unix 纪元(1970-01-01)对应的儒略日
        return 2440587.5 + epochDay
                + utc.getHour() / 24.0
                + utc.getMinute() / 1440.0
                + utc.getSecond() / 86400.0;
    }

    /**
     * 计算格林尼治平恒星时 GMST(度)。
     *
     * <p>公式(参考 IAU 1982):
     * GMST = 280.46061837° + 360.98564736629° × (JD - J2000)
     *        + 0.000387933° × T² - T³ / 38710000
     * 其中 T = (JD - J2000) / 36525(儒略世纪)。</p>
     */
    private double calculateGmst(double jd) {
        // T:自 J2000 起的儒略世纪数(36525 天 = 100 年),用于高阶修正项
        double t = (jd - J2000) / 36525.0;
        // ┌─────────────────────────────────────────────────────────┐
        // │ IAU 1982 GMST 公式(国际天文联合会标准)                  │
        // │                                                         │
        // │ 常数项 280.46061837°:J2000 时刻的 GMST 初值              │
        // │ 主项 360.98564736629° × (JD - J2000):地球自转,每天约一圈  │
        // │   注意 360.9856 > 360,因为地球自转一圈同时公转了一点,      │
        // │   相对春分点要多转一点才能回到同一位置(恒星日 < 太阳日)    │
        // │ T²、T³ 项:长期变化修正(岁差、潮汐摩擦导致自转减慢)        │
        // └─────────────────────────────────────────────────────────┘
        double gmst = 280.46061837
                + 360.98564736629 * (jd - J2000)
                + 0.000387933 * t * t
                - t * t * t / 38710000.0;
        // 规范化到 [0, 360) 度,避免角度无限增长
        return normalizeAngle(gmst);
    }

    /**
     * 赤道坐标 → 地平坐标(球面三角转换)。
     *
     * <p>核心公式:</p>
     * <ul>
     *   <li>时角 H = LST - RA(度,转弧度后参与三角运算)</li>
     *   <li>sin(Alt) = sin(Dec)·sin(Lat) + cos(Dec)·cos(Lat)·cos(H)</li>
     *   <li>cos(Az) = [sin(Dec) - sin(Alt)·sin(Lat)] / [cos(Alt)·cos(Lat)]</li>
     *   <li>当 sin(H) &gt; 0 时方位角取 360 - Az(修正象限)</li>
     * </ul>
     *
     * @param ra    赤经(度)
     * @param dec   赤纬(度)
     * @param lst   当地恒星时(度)
     * @param latRad 观测者纬度(弧度)
     * @return [azimuth, altitude] 度数
     */
    private double[] equatorialToHorizontal(double ra, double dec, double lst, double latRad) {
        // ──────────────────────────────────────────────────────────
        // 时角 H(Hour Angle):天体相对当地子午圈的角度
        //   H = LST - RA
        //   LST 是当地恒星时(子午圈位置),RA 是天体的赤经
        //   H = 0 表示天体在子午圈正上方(最高点)
        //   H > 0 表示天体已经过了子午圈(向西移动)
        // ──────────────────────────────────────────────────────────
        double haRad = Math.toRadians(normalizeAngle(lst - ra));
        double decRad = Math.toRadians(dec);

        // ┌─────────────────────────────────────────────────────────┐
        // │ 高度角 Alt 公式(球面三角余弦定律)                       │
        // │                                                         │
        // │   sin(Alt) = sin(Dec)·sin(Lat) + cos(Dec)·cos(Lat)·cos(H) │
        // │                                                         │
        // │ 直觉理解:                                               │
        // │   - sin(Dec)·sin(Lat):赤纬与纬度的「对齐」贡献            │
        // │     如果 Dec=Lat(星在观测者正上方),cos(H)=1 时 Alt=90°     │
        // │   - cos(Dec)·cos(Lat)·cos(H):随时间变化的部分             │
        // │     地球自转 H 变化,星的高度随之升降                      │
        // └─────────────────────────────────────────────────────────┘
        double sinAlt = Math.sin(decRad) * Math.sin(latRad)
                      + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        // 钳制到 [-1, 1],避免浮点误差导致 asin 返回 NaN
        sinAlt = Math.max(-1, Math.min(1, sinAlt));
        double alt = Math.toDegrees(Math.asin(sinAlt));

        double cosAlt = Math.cos(Math.toRadians(alt));
        double cosAz = 1.0;
        if (Math.abs(cosAlt) > 1e-10) {
            // ┌─────────────────────────────────────────────────────────┐
            // │ 方位角 Az 公式(由 sin(Alt) 公式反推)                    │
            // │                                                         │
            // │   cos(Az) = [sin(Dec) - sin(Alt)·sin(Lat)]              │
            // │             / [cos(Alt)·cos(Lat)]                       │
            // │                                                         │
            // │ 直觉:已知高度角后,用同样的球面三角关系反解方位角          │
            // │ 分母 cos(Alt)·cos(Lat) 接近 0 时(星在天顶或地平极点),     │
            // │ 方位角不确定,跳过计算保留默认值                          │
            // └─────────────────────────────────────────────────────────┘
            double numerator = Math.sin(decRad) - Math.sin(Math.toRadians(alt)) * Math.sin(latRad);
            double denom = cosAlt * Math.cos(latRad);
            if (Math.abs(denom) > 1e-10) {
                cosAz = numerator / denom;
                // 同样钳制到 [-1, 1],避免 acos 越界
                cosAz = Math.max(-1, Math.min(1, cosAz));
            }
        }
        double az = Math.toDegrees(Math.acos(cosAz));
        // ──────────────────────────────────────────────────────────
        // 象限修正:acos 函数只返回 [0, 180°]
        // 但方位角是 [0, 360°),需要判断星在东半球还是西半球
        // sin(H) > 0 → H 在 (0, 180°)→ 星在西侧(已过子午圈)→ Az = 360 - Az
        // sin(H) ≤ 0 → H 在 (180°, 360°)→ 星在东侧(未到子午圈)→ Az 不变
        // ──────────────────────────────────────────────────────────
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
