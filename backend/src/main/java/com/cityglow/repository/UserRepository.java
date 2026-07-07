package com.cityglow.repository;

import com.cityglow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户 Repository。
 */
public interface UserRepository extends JpaRepository<User, Long> {
}
