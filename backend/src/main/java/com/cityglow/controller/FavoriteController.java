package com.cityglow.controller;

import com.cityglow.domain.ApiResponse;
import com.cityglow.domain.FavoriteRequest;
import com.cityglow.domain.FavoriteResponse;
import com.cityglow.entity.User;
import com.cityglow.repository.UserRepository;
import com.cityglow.service.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收藏观测点 REST 接口(需认证)。
 *
 * <p>暴露:</p>
 * <ul>
 *   <li>{@code GET /api/v1/favorites} - 当前用户的全部收藏</li>
 *   <li>{@code POST /api/v1/favorites} body:{name, latitude, longitude} - 添加收藏(幂等)</li>
 *   <li>{@code DELETE /api/v1/favorites?latitude=&longitude=} - 删除收藏</li>
 * </ul>
 *
 * <p>当前用户身份从 SecurityContext 取 username,再查 User 得到 userId。
 * 异常一律包装为 RuntimeException(不显式 throws)。</p>
 */
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    /**
     * 列出当前用户的全部收藏。
     *
     * @return 收藏列表
     */
    @GetMapping
    public ApiResponse<List<FavoriteResponse>> list() {
        Long userId = currentUserId();
        return ApiResponse.success(favoriteService.list(userId));
    }

    /**
     * 添加收藏(幂等:相同坐标已存在则返回已有记录)。
     *
     * @param request {name, latitude, longitude}
     * @return 收藏响应
     */
    @PostMapping
    public ApiResponse<FavoriteResponse> add(@RequestBody FavoriteRequest request) {
        Long userId = currentUserId();
        return ApiResponse.success(favoriteService.add(userId, request));
    }

    /**
     * 按坐标删除收藏。
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 成功响应(data=null)
     */
    @DeleteMapping
    public ApiResponse<Void> remove(@RequestParam("latitude") double latitude,
                                    @RequestParam("longitude") double longitude) {
        Long userId = currentUserId();
        favoriteService.remove(userId, latitude, longitude);
        return ApiResponse.success(null);
    }

    /**
     * 从 SecurityContext 取 username,再查 User 得到 userId。
     *
     * <p>用 {@link Authentication#getName()} 取 username(与 JwtAuthenticationFilter
     * 设置的 String principal 一致),再查 User 表得到 userId。</p>
     *
     * @return 当前登录用户 id
     * @throws RuntimeException 未认证或用户不存在时
     */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // 理论上已被 SecurityFilterChain 拦截需认证,此处兜底防御
            throw new RuntimeException("Not authenticated");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return user.getId();
    }
}
