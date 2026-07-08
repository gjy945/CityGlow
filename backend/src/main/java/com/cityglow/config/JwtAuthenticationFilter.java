package com.cityglow.config;

import com.cityglow.entity.User;
import com.cityglow.repository.UserRepository;
import com.cityglow.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * JWT 认证过滤器(OncePerRequestFilter,每请求执行一次)。
 *
 * <p>从 {@code Authorization: Bearer <token>} header 取 JWT,验证后
 * 将用户身份写入 {@link SecurityContextHolder},后续 Controller 可通过
 * SecurityContext 拿到当前用户。</p>
 *
 * <p>无 token 或 token 非法时不抛异常,仅不设置认证上下文,
 * 由后续 SecurityFilterChain 决定放行或 401。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从 Authorization header 提取 Bearer token
        String token = extractToken(request);
        // 2. token 存在且签名/过期校验通过才进入认证流程
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            // 3. 从 token 载荷解析 username(subject)
            String username = jwtUtil.extractUsername(token);
            // 4. 二次校验:确保用户仍存在(防止用户被删除后旧 token 仍可用)
            Optional<User> userOpt = userRepository.findByUsername(username);
            // 5. 仅当 SecurityContext 未设置认证(避免覆盖已有认证)且用户存在时才写入
            if (userOpt.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 用 username 作为 principal,密码留空(已认证无需密码),授予普通用户角色
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // 写入 SecurityContext,后续 Controller 通过 SecurityContextHolder 取用
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        // 无 token 或校验失败也不抛异常,继续过滤链,由 SecurityFilterChain 决定放行/401
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization header 提取 Bearer token。
     *
     * @param request HTTP 请求
     * @return token 字符串,无 Bearer 前缀时返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
