package com.lucky.app.system.repository;

import com.lucky.app.system.entity.RevokedToken;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the JWT blacklist: existence check for the auth filter and expiry cleanup. */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByToken(String token);
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
