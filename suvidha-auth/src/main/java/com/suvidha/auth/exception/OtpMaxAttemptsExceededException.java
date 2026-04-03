package com.suvidha.auth.exception;

public class OtpMaxAttemptsExceededException extends ApiException {
    public OtpMaxAttemptsExceededException(String message) {
        super("OTP_MAX_ATTEMPTS", message);
    }
}
