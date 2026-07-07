package com.cityglow.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类(基于 JJWT 0.12.x)。
 *
 * <p>负责生成/验证 JWT token,使用 HS256 算法签名。
 * 密钥从 {@code jwt.secret} 配置项读取(默认值见 application.yml),
 * 长度至少 32 字节以满足 HS256 要求。</p>
 *
 * <p>token 有效期 7 天,载荷中以 subject 携带 username。</p>
 */
@Component
public class JwtUtil {

    /** token 有效期:7 天(毫秒)。 */
    private static final long EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;

    /** 签名密钥(由配置注入,运行时不可变)。 */
    private final SecretKey secretKey;

    /**
     * 构造时读取 {@code jwt.secret} 并派生 HS256 密钥。
     *
     * @param secret 配置中的 jwt.secret(至少 32 字节)
     */
    public JwtUtil(@Value("${jwt.secret:cityglow-secret-key-for-jwt-signing-must-be-at-least-256-bits-long}")
                   String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT token(以 username 为 subject,7 天过期)。
     *
     * @param username 用户名
     * @return JWT token 字符串
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 token 提取 username(subject)。
     *
     * @param token JWT token
     * @return username;token 非法/过期时抛 RuntimeException
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            // 严禁方法签名抛 checked 异常,统一包装为 RuntimeException
            throw new RuntimeException("Invalid or expired JWT token", e);
        }
    }

    /**
     * 校验 token 是否合法且未过期。
     *
     * @param token JWT token
     * @return true 合法;false 非法或过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
