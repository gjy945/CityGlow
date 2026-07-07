package com.cityglow.domain;

/**
 * 认证响应 DTO(注册/登录返回,JDK 21 Record)。
 *
 * <p>响应体示例:</p>
 * <pre>{@code
 * {
 *   "token": "eyJhbGciOi...",
 *   "user": { "id": 1, "username": "stargazer", "avatarUrl": null }
 * }
 * }</pre>
 *
 * @param token JWT token(客户端后续请求放 Authorization: Bearer <token>)
 * @param user  用户基本信息(不含密码)
 */
public record AuthResponse(
        String token,
        UserInfo user
) {
    /**
     * 用户基本信息(嵌套 record,字段刻意精简,不含 password)。
     *
     * @param id        主键
     * @param username  用户名
     * @param avatarUrl 头像 URL(可能为 null)
     */
    public record UserInfo(
            Long id,
            String username,
            String avatarUrl
    ) {
    }
}
