package com.lucky.app.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerRequest(
        @NotBlank(message = "Full name is required")
        @Pattern(regexp = "^[A-Za-z ]+$", message = "Full name must contain letters only")
        String fullName,
        @NotBlank(message = "National ID is required")
        @Pattern(regexp = "^\\d{16}$", message = "National ID must be exactly 16 digits")
        String nationalId,
        @Email(message = "Email must be valid")
        String email,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^07\\d{8}$", message = "Phone number must be 10 digits and start with 07")
        String phoneNumber,
        @NotBlank(message = "Address is required")
        String address,
        Long userId
) {
}
