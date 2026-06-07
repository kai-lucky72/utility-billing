package com.lucky.app.system.dto.response;

/** Login result: the JWT plus basic identity (role, email, name) for the client to display. */
public record AuthResponse(
        String token,
        String role,
        String email,
        String fullName
) {
}
