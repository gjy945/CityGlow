package com.cityglow.domain;

/**
 * 收藏观测点请求 DTO(JDK 21 Record)。
 *
 * <p>请求体示例:</p>
 * <pre>{@code
 * { "name": "北京灵山", "latitude": 39.9, "longitude": 116.4 }
 * }</pre>
 *
 * @param name      地点名称
 * @param latitude  纬度
 * @param longitude 经度
 */
public record FavoriteRequest(
        String name,
        double latitude,
        double longitude
) {
}
