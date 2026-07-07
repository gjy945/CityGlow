package com.cityglow.domain;

/**
 * 单颗可见星(已完成赤道→地平投影)。
 *
 * @param hip HIP 编号(亮星表唯一标识)
 * @param mag 视星等(数值越小越亮,负数代表极亮星)
 * @param az  方位角(度,0=北,90=东,180=南,270=西)
 * @param alt 高度角(度,0=地平线,90=天顶)
 */
public record StarPoint(int hip, double mag, double az, double alt) {
}
