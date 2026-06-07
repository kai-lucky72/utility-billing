package com.lucky.app.system.dto.response;

import java.time.LocalDateTime;

/** Result of sign-up: email, assigned role, whether email verification is required, and OTP expiry. */
public record RegistrationResponse(
        String email,
        String role,
        boolean verificationRequired,
        LocalDateTime otpExpiresAt
) {
}
