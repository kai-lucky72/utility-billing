package com.lucky.app.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Payload to request a fresh verification OTP for an unverified email. */
public record ResendVerificationOtpRequest(
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email
) {
}
