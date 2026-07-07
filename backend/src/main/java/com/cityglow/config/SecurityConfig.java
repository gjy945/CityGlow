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
                    .csrf(csrf -> csrf.disable())
                    .cors(cors -> {}) // 使用默认 CORS 配置
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/apod").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/logs/**").permitAll()
                            // 太空天气(极光预报):公开接口,无需认证
                            .requestMatchers(HttpMethod.GET, "/api/v1/space-weather/**").permitAll()
                            // 近地小行星:公开接口,无需认证
                            .requestMatchers(HttpMethod.GET, "/api/v1/neo/**").permitAll()
                            // 最佳观测时段:公开接口,无需认证
                            .requestMatchers(HttpMethod.GET, "/api/v1/best-window/**").permitAll()
                            .requestMatchers("/api/v1/**").authenticated()
                            .anyRequest().permitAll()
                    )
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        } catch (Exception e) {
            // HttpSecurity 构建过程中的检查异常统一包装为 RuntimeException
            throw new RuntimeException("Failed to build SecurityFilterChain", e);
        }
    }
}
