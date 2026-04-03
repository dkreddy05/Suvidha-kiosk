package com.suvidha.auth.service;

import com.suvidha.auth.Dto.VerifyOtpResponse;

public interface AuthenticationService {

    String sendOtp(String sessionId, String mobile);

    /**
     * Verifies OTP for a session.
     * Mobile is inferred from the OTP session stored in Redis.
     */
    VerifyOtpResponse verifyOtp(String sessionId, String otp);
}
