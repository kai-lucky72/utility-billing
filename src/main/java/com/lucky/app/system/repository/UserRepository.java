package com.lucky.app.system.repository;

import com.lucky.app.system.entity.User;
import com.lucky.app.system.enums.Role;
import com.lucky.app.system.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link User} accounts: lookup/existence by email and role-based queries. */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRoleIn(List<Role> roles);
    List<User> findAllByRole(Role role);
    long countByRoleAndStatus(Role role, UserStatus status);
    Page<User> findAll(Pageable pageable);
}
