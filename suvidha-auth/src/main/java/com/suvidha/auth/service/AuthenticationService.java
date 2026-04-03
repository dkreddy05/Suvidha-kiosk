package com.suvidha.auth.service;

import com.suvidha.auth.Dto.VerifyOtpResponse;

public interface AuthenticationService {

    String sendOtp(String sessionId, String mobile);

    VerifyOtpResponse verifyOtp(String sessionId, String otp);
}
