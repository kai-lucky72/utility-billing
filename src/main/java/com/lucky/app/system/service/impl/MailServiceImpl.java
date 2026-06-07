package com.lucky.app.system.service.impl;

import com.lucky.app.system.exception.BusinessRuleException;
import com.lucky.app.system.service.interfaces.MailService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * JavaMailSender-backed implementation of {@link MailService}. Verification OTPs are
 * treated as critical (failure throws), while billing notification emails are best-effort
 * (failure is logged and swallowed). Sending is globally toggled by {@code app.mail.enabled}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final DateTimeFormatter OTP_EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendVerificationOtp(String recipientEmail, String recipientName, String otpCode, LocalDateTime expiresAt) {
        if (!mailEnabled) {
            log.warn("Email delivery is disabled. Verification OTP for {} was not sent.", recipientEmail);
            throw new BusinessRuleException("Email verification is currently unavailable. Please contact support.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Utility Billing System email verification code");
        message.setText("""
                Hello %s,

                Your Utility Billing System verification code is: %s

                This code expires at %s.

                If you did not create this account, please ignore this email.
                """.formatted(
                recipientName,
                otpCode,
                expiresAt.format(OTP_EXPIRY_FORMATTER)
        ));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send verification OTP to {}", recipientEmail, ex);
            throw new BusinessRuleException("Unable to send verification email right now. Please try again.");
        }
    }

    @Override
    public void sendNotificationEmail(String recipientEmail, String recipientName, String subject, String body) {
        // Best-effort: never let an email problem roll back a bill/payment. Skip quietly when
        // mail is disabled or the customer has no email on file; log (don't throw) on send errors.
        if (!mailEnabled) {
            log.info("Email disabled; skipping notification email to {}", recipientEmail);
            return;
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.info("No email address on file; skipping notification '{}'", subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText("Hello %s,%n%n%s%n%n— Utility Billing System"
                .formatted(recipientName == null ? "Customer" : recipientName, body));

        try {
            mailSender.send(message);
            log.info("Notification email sent to {} ({})", recipientEmail, subject);
        } catch (MailException ex) {
            log.error("Failed to send notification email to {}", recipientEmail, ex);
        }
    }
}
