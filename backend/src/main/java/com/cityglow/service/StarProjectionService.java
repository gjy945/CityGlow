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
 * 星空投影服务 —— 算出"我现在抬头能看到哪些星,它们在天空的什么位置"。
 *
 * <p>打个比方:</p>
 * <ul>
 *   <li>每颗星在天上有一个"固定地址"(赤经 RA + 赤纬 Dec),永远不变</li>
 *   <li>但你在地上抬头看,看到的位置会变 —— 因为地球在转</li>
 *   <li>所以要把"固定地址"换算成"我现在眼里看到的位置"(方位角 + 高度角)</li>
 * </ul>
 *
 * <p>方位角 = 东西南北哪个方向(0=北,90=东,180=南,270=西)
 * 高度角 = 抬头仰角(0=地平线,90=正头顶)</p>
 */
@Service
public class StarProjectionService {

    // J2000:天文学约定的一个"起算时刻",2000年1月1日中午
    private static final double J2000 = 2451545.0;

    /**
     * 主方法:给定位置+时间,算出能看到哪些星。
     *
     * 打个比方:你告诉它"我在北京,今晚10点",它返回一张星图,
     * 告诉你猎户座在东南方、仰角30度的位置。
     */
    public List<StarPoint> project(
            double latitude,
            double longitude,
            LocalDate date,
            int hour,
            List<StarRecord> stars) {

        // ═══════ 第1步:把日期时间转成一个连续的数字(儒略日) ═══════
        // 儒略日就是"从很久很久以前某一天开始,数了多少天"
        // 为什么不用"2026年7月8日"?因为月份天数不一样、有闰年,算起来麻烦
        // 用连续数字就简单了,两个日期相减就知道隔了多少天
        double jd = toJulianDate(date, hour);

        // ═══════ 第2步:算地球现在转到了什么角度(GMST) ═══════
        // 地球在自转,星空看起来在转。GMST 就是"现在地球转到了哪个位置"
        double gmst = calculateGmst(jd);

        // ═══════ 第3步:算你所在位置的"当地时间角度"(LST) ═══════
        // GMST 是格林尼治(伦敦)的角度,你在北京要加上你的经度
        // 比如北京在东经116度,就加上116,得到你当地的"天空旋转角度"
        double lst = normalizeAngle(gmst + longitude);

        // 纬度转弧度,因为后面的三角函数要用弧度
        double latRad = Math.toRadians(latitude);

        List<StarPoint> visible = new ArrayList<>();
        // ═══════ 第4步:逐颗星算位置 ═══════
        for (StarRecord star : stars) {
            // 把这颗星的"固定地址"换算成"你现在看到的位置"
            double[] azAlt = equatorialToHorizontal(star.ra(), star.dec(), lst, latRad);
            double az = azAlt[0];  // 方位角:东西南北
            double alt = azAlt[1]; // 高度角:仰角
            // ═══════ 第5步:只留地平线以上的星 ═══════
            // 高度角 ≤ 0 说明星在地平线下面,你看不到,扔掉
            if (alt > 0) {
                visible.add(new StarPoint(star.hip(), star.mag(), az, alt));
            }
        }
        return visible;
    }

    /**
     * 把日期时间转成儒略日(连续数字)。
     *
     * 打个比方:就像把"2026年7月8日"翻译成一个数字"2461044.5",
     * 这样后面算时间差就方便了,直接两个数字相减。
     */
    private double toJulianDate(LocalDate date, int hour) {
        LocalDateTime local = date.atTime(hour, 0);
        // 北京时间 → 伦敦时间(UTC),因为天文学公式都用伦敦时间
        ZonedDateTime utc = local.atZone(ZoneId.of("Asia/Shanghai"))
                .withZoneSameInstant(ZoneOffset.UTC);
        long epochDay = utc.toLocalDate().toEpochDay();
        // 2440587.5 是 1970年1月1日 对应的儒略日
        // epochDay 是从1970年1月1日开始数的天数
        // 两个加起来就是儒略日
        return 2440587.5 + epochDay
                + utc.getHour() / 24.0
                + utc.getMinute() / 1440.0
                + utc.getSecond() / 86400.0;
    }

    /**
     * 算地球现在转到了什么角度(GMST)。
     *
     * 打个比方:地球像一个旋转的盘子,GMST 就是"盘子现在转到了几度"。
     * 这个角度决定了现在哪片天空对着我们。
     */
    private double calculateGmst(double jd) {
        // t:从2000年到现在过了多少个"百年"(36525天 = 100年)
        double t = (jd - J2000) / 36525.0;
        // 这个公式是天文学标准公式(IAU 1982),不用深究
        // 简单说:基数 + 每天转过的角度 × 天数 + 一点点修正
        // 360.9856 比正好360多一点点,因为地球自转一圈的同时还绕太阳走了一点点,
        // 所以一天下来相对星星要多转一点才能回到原位
        double gmst = 280.46061837
                + 360.98564736629 * (jd - J2000)
                + 0.000387933 * t * t
                - t * t * t / 38710000.0;
        // 把角度限制在 0-360 度之间(转了好几圈就只取最后一圈)
        return normalizeAngle(gmst);
    }

    /**
     * 核心:把星的"固定地址"换算成"你现在看到的位置"。
     *
     * 打个比方:
     * - 赤经RA、赤纬Dec = 星在天上的永久住址
     * - 方位角Az、高度角Alt = 你此刻抬头,这颗星在你眼里在哪
     *
     * 中间的桥梁是"时角H":这颗星现在偏离子午线(你头顶的正南北线)多远
     */
    private double[] equatorialToHorizontal(double ra, double dec, double lst, double latRad) {
        // ──────────────────────────────────────────────
        // 第1步:算时角 H
        // 时角 = 当地天空角度(LST) - 星的赤经(RA)
        // 意思:这颗星现在偏离子午线(你头顶南北那条线)多远
        // H=0:星正好在子午线上,也就是你头顶最高点
        // H>0:星已经过了最高点,正在往西落
        // ──────────────────────────────────────────────
        double haRad = Math.toRadians(normalizeAngle(lst - ra));
        double decRad = Math.toRadians(dec);

        // ──────────────────────────────────────────────
        // 第2步:算高度角(仰角)Alt
        // 公式:sin(Alt) = sin(Dec)·sin(Lat) + cos(Dec)·cos(Lat)·cos(H)
        //
        // 这个公式不用死记,直观理解:
        // - 前半段 sin(Dec)·sin(Lat):星的赤纬和你所在纬度"重合度"有多高
        //   如果星正好在你头顶(Dec=Lat),那它就在天顶,高度角=90度
        // - 后半段 cos(H):随时间变化的部分,地球转H就变,星就升升落落
        // ──────────────────────────────────────────────
        double sinAlt = Math.sin(decRad) * Math.sin(latRad)
                      + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        // 安全检查:把 sinAlt 限制在 [-1, 1] 之间
        // 因为浮点运算可能有微小误差,sinAlt 变成 1.0000001 的话 asin 就会报错
        sinAlt = Math.max(-1, Math.min(1, sinAlt));
        double alt = Math.toDegrees(Math.asin(sinAlt));

        // ──────────────────────────────────────────────
        // 第3步:算方位角(东西南北)Az
        // 公式:cos(Az) = [sin(Dec) - sin(Alt)·sin(Lat)] / [cos(Alt)·cos(Lat)]
        //
        // 就是把第2步的公式反过来用:已知高度角,反推方位角
        // ──────────────────────────────────────────────
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

        // ──────────────────────────────────────────────
        // 第4步:象限修正(容易踩坑的地方!)
        //
        // acos 这个函数只能返回 0-180 度,也就是只能区分"南"和"北"
        // 但方位角是 0-360 度,还要区分"东"和"西"
        //
        // 怎么判断东西?看时角 H 的正负:
        // - sin(H) > 0:星在西边 → 方位角 = 360 - Az
        // - sin(H) ≤ 0:星在东边 → 方位角不变
        //
        // 不修正的话,西半边的星会画到东边去,整个星图就错了
        // ──────────────────────────────────────────────
        if (Math.sin(haRad) > 0) {
            az = 360 - az;
        }
        return new double[]{normalizeAngle(az), alt};
    }

    /** 把角度限制在 0-360 度之间(超过360就减掉360,负数就加360)。 */
    private double normalizeAngle(double deg) {
        double r = deg % 360;
        if (r < 0) r += 360;
        return r;
    }
}
