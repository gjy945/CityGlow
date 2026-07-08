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
 * 用户实体,对应 users 表。
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名(唯一,注册时校验)。 */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * 密码字段:存 BCrypt 哈希(由 AuthService 在注册/登录时填充),不存明文。
     */
    @Column(nullable = false, length = 100)
    private String password;

    /** 头像 URL(可选,当前未启用上传,默认 null)。 */
    @Column(name = "avatar_url")
    private String avatarUrl;

    /** 创建时间(Hibernate 自动注入,不可更新)。 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
