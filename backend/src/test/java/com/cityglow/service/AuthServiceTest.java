package com.cityglow.service;

import com.cityglow.domain.AuthResponse;
import com.cityglow.entity.User;
import com.cityglow.repository.UserRepository;
import com.cityglow.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService 单元测试。
 *
 * <p>用 Mockito mock {@link UserRepository}、{@link PasswordEncoder}、{@link JwtUtil},
 * 不真实写库、不真实哈希、不真实签发 JWT。</p>
 *
 * <p>覆盖:</p>
 * <ul>
 *   <li>register 成功路径:用户名未占用 → BCrypt 哈希 → 保存 → 生成 JWT</li>
 *   <li>register 用户名已占用 → 抛 RuntimeException,不保存</li>
 *   <li>login 成功路径:用户存在 + 密码匹配 → 生成 JWT</li>
 *   <li>login 用户不存在 → 抛 RuntimeException</li>
 *   <li>login 密码错误 → 抛 RuntimeException</li>
 *   <li>getCurrentUser 成功/失败路径</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_newUsername_savesUserAndReturnsToken() {
        // given:用户名未占用
        when(userRepository.findByUsername("stargazer")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("p@ssw0rd")).thenReturn("hashed-pwd");
        // save 时给一个带 id 的 User 模拟自增主键
        User saved = buildUser(1L, "stargazer", "hashed-pwd");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtUtil.generateToken("stargazer")).thenReturn("jwt-token");

        // when
        AuthResponse resp = authService.register("stargazer", "p@ssw0rd");

        // then
        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.user().id()).isEqualTo(1L);
        assertThat(resp.user().username()).isEqualTo("stargazer");
        assertThat(resp.user().avatarUrl()).isNull();
        verify(passwordEncoder, times(1)).encode("p@ssw0rd");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken("stargazer");
    }

    @Test
    void register_existingUsername_throwsAndDoesNotSave() {
        // given:用户名已占用
        User existing = buildUser(1L, "stargazer", "hashed-pwd");
        when(userRepository.findByUsername("stargazer")).thenReturn(Optional.of(existing));

        // when + then
        assertThatThrownBy(() -> authService.register("stargazer", "p@ssw0rd"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");

        // 验证未发生写库/哈希
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void login_validCredentials_returnsToken() {
        // given
        User user = buildUser(1L, "stargazer", "hashed-pwd");
        when(userRepository.findByUsername("stargazer")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("p@ssw0rd", "hashed-pwd")).thenReturn(true);
        when(jwtUtil.generateToken("stargazer")).thenReturn("jwt-token");

        // when
        AuthResponse resp = authService.login("stargazer", "p@ssw0rd");

        // then
        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.user().id()).isEqualTo(1L);
        assertThat(resp.user().username()).isEqualTo("stargazer");
    }

    @Test
    void login_userNotFound_throwsRuntimeException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost", "any"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void login_invalidPassword_throwsRuntimeException() {
        User user = buildUser(1L, "stargazer", "hashed-pwd");
        when(userRepository.findByUsername("stargazer")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("stargazer", "wrong"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid password");

        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void getCurrentUser_existingUsername_returnsUserInfo() {
        User user = buildUser(1L, "stargazer", "hashed-pwd");
        when(userRepository.findByUsername("stargazer")).thenReturn(Optional.of(user));

        AuthResponse.UserInfo info = authService.getCurrentUser("stargazer");

        assertThat(info.id()).isEqualTo(1L);
        assertThat(info.username()).isEqualTo("stargazer");
        assertThat(info.avatarUrl()).isNull();
    }

    @Test
    void getCurrentUser_unknownUsername_throwsRuntimeException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    /**
     * 构造测试用 User 实例(模拟持久化后状态,id 非空)。
     */
    private User buildUser(Long id, String username, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
