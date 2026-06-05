package com.lucky.app.system.dto.response;

public record AuthResponse(
        String token,
        String role,
        String email,
        String fullName
) {
}
