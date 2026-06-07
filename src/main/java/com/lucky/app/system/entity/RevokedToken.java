package com.lucky.app.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A JWT that has been blacklisted by logout. The auth filter rejects any token listed here;
 * expired entries are purged on a schedule.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken extends BaseEntity {

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
