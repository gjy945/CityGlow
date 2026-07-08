package com.cityglow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 收藏观测点实体,对应 favorite_locations 表。
 *
 * <p>用户收藏的观测地点(经纬度 + 名称),同一用户下经纬度唯一(幂等)。</p>
 */
@Entity
@Table(name = "favorite_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联用户 id。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 地点名称(用户自定义,如"北京灵山")。 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 纬度。 */
    @Column(nullable = false)
    private double latitude;

    /** 经度。 */
    @Column(nullable = false)
    private double longitude;

    /** 创建时间(Hibernate 自动注入,不可更新)。 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
