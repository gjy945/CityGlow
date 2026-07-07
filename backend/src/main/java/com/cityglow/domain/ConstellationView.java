package com.cityglow.domain;

import java.util.List;

/**
 * 单个星座的可见视图(已投影)。
 *
 * @param name   星座英文标识(如 "orion")
 * @param latin  拉丁名(如 "Orion")
 * @param chinese 中文名(如 "猎户座")
 * @param stars  星座内的星点列表(已投影到地平坐标)
 * @param lines  连线索引对,每条 [i, j] 引用 stars 列表的索引
 */
public record ConstellationView(
        String name,
        String latin,
        String chinese,
        List<StarPoint> stars,
        List<int[]> lines
) {
    public ConstellationView {
        stars = (stars == null) ? List.of() : stars;
        lines = (lines == null) ? List.of() : lines;
    }
}
