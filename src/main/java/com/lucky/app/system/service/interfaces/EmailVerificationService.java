package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.response.OtpDispatchResponse;
import com.lucky.app.system.entity.User;

/** Contract for issuing, resending, verifying, and cleaning up email-verification OTPs. */
public interface EmailVerificationService {

    OtpDispatchResponse issueVerificationOtp(User user);

    OtpDispatchResponse resendVerificationOtp(String email);

    void verifyEmailOtp(String email, String otpCode);

    void cleanupExpiredOtps();
}
