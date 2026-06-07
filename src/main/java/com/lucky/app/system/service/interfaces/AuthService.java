package com.lucky.app.system.service.interfaces;

import com.lucky.app.system.dto.request.LoginRequest;
import com.lucky.app.system.dto.request.RegisterRequest;
import com.lucky.app.system.dto.request.ResendVerificationOtpRequest;
import com.lucky.app.system.dto.request.VerifyEmailOtpRequest;
import com.lucky.app.system.dto.response.AuthResponse;
import com.lucky.app.system.dto.response.OtpDispatchResponse;
import com.lucky.app.system.dto.response.RegistrationResponse;
import com.lucky.app.system.dto.response.UserResponse;

/** Contract for authentication: register, email verification, login/logout, and current-user lookup. */
public interface AuthService {
    RegistrationResponse register(RegisterRequest request);
    void verifyEmailOtp(VerifyEmailOtpRequest request);
    OtpDispatchResponse resendVerificationOtp(ResendVerificationOtpRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String authorizationHeader);
    UserResponse me();
}
