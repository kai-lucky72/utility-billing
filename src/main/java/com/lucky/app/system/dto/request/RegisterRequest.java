package com.lucky.app.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Pattern(regexp = "^[A-Za-z ]+$", message = "Full name must contain letters only")
        String fullName,
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^07\\d{8}$", message = "Phone number must be 10 digits and start with 07")
        String phoneNumber,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
