package com.cityglow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置(设计文档第 4 节模块 1:用户认证)。
 *
 * <p>规则:</p>
 * <ul>
 *   <li>放行:GET /api/v1/events/**、GET /api/v1/apod、POST /api/v1/auth/**、
 *       GET /uploads/**、GET /api/v1/logs/**</li>
 *   <li>其余 /api/v1/** 需认证</li>
 *   <li>禁用 CSRF(CORS 用 default,前后端分离场景由前端处理跨域)</li>
 *   <li>无状态 session(JWT 自带身份,不创建 HttpSession)</li>
 *   <li>在 UsernamePasswordAuthenticationFilter 之前插入 JwtAuthenticationFilter</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器:BCrypt(CityGlow 用户密码哈希算法)。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置 SecurityFilterChain。
     *
     * @param http HttpSecurity 构建器
     * @return 安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                    // 禁用 CSRF:前后端分离 + JWT 无状态,无 cookie session 需要 CSRF 保护
                    .csrf(csrf -> csrf.disable())
                    // 启用默认 CORS 配置(允许跨域,前端独立部署)
                    .cors(cors -> {})
                    // 无状态 session:JWT 自带身份,不创建/不使用 HttpSession
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            // 公开只读接口:天文事件、APOD、星图、日志列表、上传图片静态资源
                            .requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/apod").permitAll()
                            // 认证接口本身必须放行,否则无法登录注册
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                            // 上传的明信片图片静态资源公开访问
                            .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/logs/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/sky/**").permitAll()
                            // 其余 /api/v1/** 必须认证(收藏的增删查等)
                            .requestMatchers("/api/v1/**").authenticated()
                            // 非 /api/v1 路径放行(静态资源、swagger 等)
                            .anyRequest().permitAll()
                    )
                    // JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前,
                    // 让请求在到达表单登录过滤器前先尝试 JWT 认证
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        } catch (Exception e) {
            // HttpSecurity 构建过程中的检查异常统一包装为 RuntimeException
            throw new RuntimeException("Failed to build SecurityFilterChain", e);
        }
    }
}
