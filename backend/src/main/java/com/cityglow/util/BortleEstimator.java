package com.cityglow.util;

/**
 * Bortle 暗空等级估算工具类(简化版)。
 *
 * <p>设计文档第 4 节模块 2 要求"查 Bortle 等级(按经纬度查表,简化版)"。
 * 由于无现成光污染瓦片数据源,这里采用粗粒度查表:已知暗夜公园/天文台
 * 坐标附近返回低 Bortle 值,其他位置返回中等光污染默认值 5。</p>
 *
 * <p><b>未来扩展点</b>:接入真实光污染数据源(如 Light Pollution Map 瓦片、
 * VIIRS 卫星夜间灯光数据),将 {@link #estimate(double, double)} 替换为
 * 基于栅格查表的精确估算。方法签名保持不变,调用方无感知。</p>
 */
public final class BortleEstimator {

    private BortleEstimator() {
        // 工具类,禁止实例化
    }

    /**
     * 估算给定经纬度的 Bortle 暗空等级(1-9)。
     *
     * <p>Bortle 等级:1=极佳暗空(荒野),9=内城天空。</p>
     *
     * @param lat 纬度
     * @param lng 经度
     * @return Bortle 等级 1-9
     */
    public static int estimate(double lat, double lng) {
        // 已知暗夜公园/天文台坐标(粗粒度,容差见各条注释)
        // 阿里天文台(西藏噶尔): 32.5, 80.0 → Bortle 1(容差 2 度)
        if (isNear(lat, lng, 32.5, 80.0, 2.0)) {
            return 1;
        }
        // 兴隆天文台(河北): 40.4, 117.6 → Bortle 3(容差 1 度)
        if (isNear(lat, lng, 40.4, 117.6, 1.0)) {
            return 3;
        }
        // 默认:中等光污染(郊区边缘)
        return 5;
    }

    /**
     * 判断 (lat1,lng1) 是否在 (lat2,lng2) 的 tolerance 度范围内。
     */
    private static boolean isNear(double lat1, double lng1,
                                  double lat2, double lng2,
                                  double tolerance) {
        return Math.abs(lat1 - lat2) < tolerance
                && Math.abs(lng1 - lng2) < tolerance;
    }
}
