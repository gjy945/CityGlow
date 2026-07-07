package com.cityglow.domain;

import com.cityglow.entity.FavoriteLocation;

import java.time.LocalDateTime;

/**
 * 收藏观测点响应 DTO(JDK 21 Record)。
 *
 * <p>响应体示例:</p>
 * <pre>{@code
 * { "id": 1, "name": "北京灵山", "latitude": 39.9, "longitude": 116.4, "createdAt": "2026-07-07T22:30:00" }
 * }</pre>
 *
 * @param id        主键
 * @param name      地点名称
 * @param latitude  纬度
 * @param longitude 经度
 * @param createdAt 收藏时间
 */
public record FavoriteResponse(
        Long id,
        String name,
        double latitude,
        double longitude,
        LocalDateTime createdAt
) {
    /**
     * 将实体转换为响应 DTO。
     *
     * @param entity 收藏实体
     * @return 响应 DTO
     */
    public static FavoriteResponse from(FavoriteLocation entity) {
        return new FavoriteResponse(
                entity.getId(),
                entity.getName(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getCreatedAt()
        );
    }
}
