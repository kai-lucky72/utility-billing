package com.lucky.app.system.dto.response;

import java.time.LocalDateTime;

/** Result of issuing/resending an OTP: the target email and when the code expires. */
public record OtpDispatchResponse(
        String email,
        LocalDateTime otpExpiresAt
) {
}
