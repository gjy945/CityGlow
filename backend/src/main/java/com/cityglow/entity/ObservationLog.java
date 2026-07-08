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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 观星日志实体,对应 observation_logs 表。
 *
 * <p>经纬度使用 BigDecimal 保留精度;bortle_level 为暗夜等级(1-9)。</p>
 */
@Entity
@Table(name = "observation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ObservationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联用户 id(当前无登录态暂留 null,后续接入认证时回填)。 */
    @Column(name = "user_id")
    private Long userId;

    /** 地点名称(用户自定义,如"北京灵山")。 */
    @Column(name = "location_name", length = 100)
    private String locationName;

    /** 纬度(BigDecimal 精度 10,7 位小数,避免 double 浮点误差)。 */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    /** 经度(BigDecimal 精度 10,7 位小数)。 */
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /** 明信片图片访问 URL(形如 /uploads/<id>.jpg)。 */
    @Column(name = "image_url")
    private String imageUrl;

    /** Bortle 暗空等级 1-9(由 PostcardService 水印元数据回填)。 */
    @Column(name = "bortle_level")
    private Integer bortleLevel;

    /** 用户备注描述(可选)。 */
    @Column(length = 500)
    private String description;

    /** 创建时间(Hibernate 自动注入,不可更新)。 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
