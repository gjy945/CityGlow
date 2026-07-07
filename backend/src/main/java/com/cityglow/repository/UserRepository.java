package com.cityglow.repository;

import com.cityglow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户 Repository。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 按用户名查找用户(用于登录/注册查重)。
     *
     * @param username 用户名
     * @return 用户 Optional,不存在返回 empty
     */
    Optional<User> findByUsername(String username);
}
