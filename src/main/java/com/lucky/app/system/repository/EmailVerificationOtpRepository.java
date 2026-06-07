package com.lucky.app.system.repository;

import com.lucky.app.system.entity.EmailVerificationOtp;
import com.lucky.app.system.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link EmailVerificationOtp}s: active codes per user, latest code, and expiry cleanup. */
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {

    List<EmailVerificationOtp> findAllByUserAndUsedFalse(User user);

    Optional<EmailVerificationOtp> findFirstByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
