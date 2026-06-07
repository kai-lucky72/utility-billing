package com.lucky.app.system.service.interfaces;

import java.time.LocalDateTime;

/**
 * Sends outbound emails for the system: account verification OTPs and customer
 * billing notifications (bill generated, payment received, bill paid, overdue).
 */
public interface MailService {

    /** Sends the email-verification OTP code to a newly registered user. */
    void sendVerificationOtp(String recipientEmail, String recipientName, String otpCode, LocalDateTime expiresAt);

    /**
     * Sends a billing notification email to a customer. Best-effort: a delivery
     * failure (or disabled mail / missing address) is logged and swallowed so it
     * never breaks the billing or payment transaction that triggered it.
     */
    void sendNotificationEmail(String recipientEmail, String recipientName, String subject, String body);
}
