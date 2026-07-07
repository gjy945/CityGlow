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
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists: " + username);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
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
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
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
