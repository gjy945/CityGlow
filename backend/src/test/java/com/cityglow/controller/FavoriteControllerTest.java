package com.cityglow.controller;

import com.cityglow.domain.FavoriteRequest;
import com.cityglow.domain.FavoriteResponse;
import com.cityglow.entity.User;
import com.cityglow.repository.UserRepository;
import com.cityglow.service.FavoriteService;
import com.cityglow.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FavoriteController Web 层测试(@WebMvcTest + MockMvc)。
 *
 * <p>用 {@link MockBean} mock {@link FavoriteService} 与 {@link UserRepository},
 * 不加载完整 Spring 上下文。{@code addFilters=false} 关闭安全过滤器链,
 * 用 {@link WithMockUser} 注入认证上下文(username=stargazer),模拟 JWT 已通过校验。</p>
 *
 * <p>验证:</p>
 * <ul>
 *   <li>GET /api/v1/favorites → 200 + 倒序列表</li>
 *   <li>POST /api/v1/favorites → 200 + 单条响应</li>
 *   <li>DELETE /api/v1/favorites?latitude=&longitude= → 200 + data=null</li>
 * </ul>
 */
@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private UserRepository userRepository;

    // 仅为满足 JwtAuthenticationFilter 自动装配(切片外依赖)
    @MockBean
    private JwtUtil jwtUtil;

    /**
     * GET /api/v1/favorites → 200,返回当前用户的收藏列表。
     */
    @Test
    @WithMockUser(username = "stargazer")
    void list_returnsCurrentUserFavorites() throws Exception {
        // given:UserRepository 查到 stargazer → id=1;Service 返回两条
        when(userRepository.findByUsername("stargazer"))
                .thenReturn(Optional.of(buildUser(1L, "stargazer")));
        when(favoriteService.list(1L)).thenReturn(List.of(
                new FavoriteResponse(2L, "天津", 39.1, 117.2,
                        LocalDateTime.of(2026, 7, 7, 22, 30)),
                new FavoriteResponse(1L, "北京灵山", 39.9, 116.4,
                        LocalDateTime.of(2026, 7, 6, 21, 0))
        ));

        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].name").value("天津"))
                .andExpect(jsonPath("$.data[1].id").value(1));
    }

    /**
     * POST /api/v1/favorites body:{name, latitude, longitude} → 200 + 单条响应。
     */
    @Test
    @WithMockUser(username = "stargazer")
    void add_returnsCreatedFavorite() throws Exception {
        when(userRepository.findByUsername("stargazer"))
                .thenReturn(Optional.of(buildUser(1L, "stargazer")));
        when(favoriteService.add(eq(1L),
                org.mockito.ArgumentMatchers.any(FavoriteRequest.class)))
                .thenReturn(new FavoriteResponse(3L, "青海冷湖", 38.8, 93.3,
                        LocalDateTime.of(2026, 7, 7, 22, 30)));

        String body = """
                {"name":"青海冷湖","latitude":38.8,"longitude":93.3}
                """;

        mockMvc.perform(post("/api/v1/favorites")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("青海冷湖"))
                .andExpect(jsonPath("$.data.latitude").value(38.8))
                .andExpect(jsonPath("$.data.longitude").value(93.3));
    }

    /**
     * DELETE /api/v1/favorites?latitude=&longitude= → 200 + data=null,Service 被调用一次。
     */
    @Test
    @WithMockUser(username = "stargazer")
    void remove_delegatesToServiceAndReturns200() throws Exception {
        when(userRepository.findByUsername("stargazer"))
                .thenReturn(Optional.of(buildUser(1L, "stargazer")));

        mockMvc.perform(delete("/api/v1/favorites")
                        .param("latitude", "39.9")
                        .param("longitude", "116.4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(favoriteService, times(1)).remove(eq(1L), anyDouble(), anyDouble());
    }

    /**
     * 构造测试用 User(模拟持久化后状态,id 非空)。
     */
    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
