package com.lucky.app.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Self-service payload for a customer to complete their own profile (national ID + address). */
public record CustomerProfileRequest(
        @NotBlank(message = "National ID is required")
        @Pattern(regexp = "^\\d{16}$", message = "National ID must be exactly 16 digits")
        String nationalId,
        @NotBlank(message = "Address is required")
        String address
) {
}
