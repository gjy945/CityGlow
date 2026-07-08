package com.cityglow.service;

import com.cityglow.domain.AuthResponse;
import com.cityglow.entity.User;
import com.cityglow.repository.UserRepository;
import com.cityglow.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户认证服务(设计文档第 4 节模块 1)。
 *
 * <p>职责:</p>
 * <ul>
 *   <li>{@link #register}:检查用户名是否已存在 → BCrypt 哈希密码 → 保存 User → 生成 JWT</li>
 *   <li>{@link #login}:按用户名查找 → BCrypt 校验密码 → 生成 JWT</li>
 *   <li>{@link #getCurrentUser}:根据 SecurityContext 中的 username 查询当前用户信息</li>
 * </ul>
 *
 * <p>所有异常一律包装为 RuntimeException(避免方法签名 throws 污染)。</p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册新用户。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 认证响应(token + 用户信息)
     * @throws RuntimeException 用户名已存在时
     */
    public AuthResponse register(String username, String password) {
        // 注册前先检查用户名是否已占用,避免唯一约束异常
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists: " + username);
        }
        User user = new User();
        user.setUsername(username);
        // BCrypt 哈希:自带盐,强度 10(默认),不可逆,校验时用 matches 比对
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
        // 注册成功即签发 JWT,免再走一次登录
        String token = jwtUtil.generateToken(username);
        return new AuthResponse(token, toUserInfo(saved));
    }

    /**
     * 用户登录。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 认证响应(token + 用户信息)
     * @throws RuntimeException 用户名不存在或密码错误时
     */
    public AuthResponse login(String username, String password) {
        // 按用户名查找,不存在直接失败(不区分"用户不存在"与"密码错误",降低撞库信息泄露)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        // BCrypt.matches(明文, 哈希):内部重新算盐+哈希后比对,恒定时间比较防时序攻击
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        // 校验通过签发 JWT,后续请求由 JwtAuthenticationFilter 解析并设置 SecurityContext
        String token = jwtUtil.generateToken(username);
        return new AuthResponse(token, toUserInfo(user));
    }

    /**
     * 根据用户名查询当前登录用户信息(供 /auth/me 使用)。
     *
     * @param username 用户名(来自 SecurityContext)
     * @return 用户基本信息
     * @throws RuntimeException 用户不存在时
     */
    public AuthResponse.UserInfo getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return toUserInfo(user);
    }

    /**
     * User entity → AuthResponse.UserInfo(剥离 password 等敏感字段)。
     */
    private AuthResponse.UserInfo toUserInfo(User user) {
        return new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getAvatarUrl());
    }
}
