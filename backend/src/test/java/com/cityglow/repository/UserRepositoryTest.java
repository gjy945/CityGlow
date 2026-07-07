package com.cityglow.repository;

import com.cityglow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 集成测试。
 *
 * <p>使用 @DataJpaTest 加载 JPA 切片,test profile 提供 H2(MySQL 模式)内存库。
 * 每个测试方法默认 @Transactional rollback,互不影响。</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindById_returnsUserWithCorrectUsername() {
        User user = new User();
        user.setUsername("stargazer");

        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("stargazer");
    }
}
