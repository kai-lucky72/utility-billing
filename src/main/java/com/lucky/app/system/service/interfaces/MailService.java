package com.lucky.app.system.service.interfaces;

import java.time.LocalDateTime;

public interface MailService {

    void sendVerificationOtp(String recipientEmail, String recipientName, String otpCode, LocalDateTime expiresAt);
}
