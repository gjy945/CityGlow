package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.domain.AuthRequest;
import com.cityglow.domain.AuthResponse;
import com.cityglow.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证 REST 接口(设计文档第 4 节模块 1)。
 *
 * <p>暴露:</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/register} body:{username,password} → {token, user:{...}}</li>
 *   <li>{@code POST /api/v1/auth/login} body:{username,password} → 同上</li>
 *   <li>{@code GET /api/v1/auth/me}(需认证) → 当前登录用户信息</li>
 * </ul>
 *
 * <p>注册/登录失败时抛 RuntimeException,由全局异常处理转 500/业务码
 * (保持与现有 Controller 风格一致,不显式 throws)。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册新用户。
     *
     * @param request {username, password}
     * @return {token, user}
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody AuthRequest request) {
        return ApiResponse.success(authService.register(request.username(), request.password()));
    }

    /**
     * 用户登录。
     *
     * @param request {username, password}
     * @return {token, user}
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest request) {
        return ApiResponse.success(authService.login(request.username(), request.password()));
    }

    /**
     * 查询当前登录用户信息(需认证,JWT 由 SecurityFilterChain 校验)。
     *
     * @return 当前用户基本信息(不含密码)
     */
    @GetMapping("/me")
    public ApiResponse<AuthResponse.UserInfo> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof String username)) {
            // 理论上 /me 已被 SecurityFilterChain 拦截需认证,此处兜底防御
            throw new RuntimeException("Not authenticated");
        }
        return ApiResponse.success(authService.getCurrentUser(username));
    }
}
