package com.cityglow.domain;

/**
 * 认证请求 DTO(注册/登录共用,JDK 21 Record)。
 *
 * <p>请求体示例:</p>
 * <pre>{@code
 * { "username": "stargazer", "password": "p@ssw0rd" }
 * }</pre>
 *
 * @param username 用户名
 * @param password 明文密码(传输层由 HTTPS 保护,服务端 BCrypt 哈希存储)
 */
public record AuthRequest(
        String username,
        String password
) {
}
