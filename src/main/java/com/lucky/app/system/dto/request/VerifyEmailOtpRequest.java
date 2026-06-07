package com.lucky.app.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Payload to verify an email: the address plus the 6-digit OTP that was emailed. */
public record VerifyEmailOtpRequest(
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "OTP code is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP code must be a 6-digit number")
        String otpCode
) {
}
