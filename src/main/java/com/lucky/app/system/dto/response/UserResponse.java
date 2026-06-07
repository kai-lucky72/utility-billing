package com.lucky.app.system.dto.response;

import com.lucky.app.system.enums.Role;
import com.lucky.app.system.enums.CustomerStatus;
import com.lucky.app.system.enums.UserStatus;
import java.time.LocalDateTime;

/** API view of a login user: identity, role, verification/status, and linked customer (if any). */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Role role,
        boolean emailVerified,
        UserStatus status,
        Long customerId,
        CustomerStatus customerStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
