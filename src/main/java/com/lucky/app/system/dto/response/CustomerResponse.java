package com.lucky.app.system.dto.response;

import com.lucky.app.system.enums.CustomerStatus;
import java.time.LocalDateTime;

/** API view of a customer: profile fields, status, linked user id, and timestamps. */
public record CustomerResponse(
        Long id,
        String fullName,
        String nationalId,
        String email,
        String phoneNumber,
        String address,
        CustomerStatus status,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
