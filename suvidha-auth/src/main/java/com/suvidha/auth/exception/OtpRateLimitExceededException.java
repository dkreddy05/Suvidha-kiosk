package com.suvidha.auth.exception;

public class OtpRateLimitExceededException extends ApiException {
    public OtpRateLimitExceededException(String message) {
        super("OTP_RATE_LIMIT", message);
    }
}
