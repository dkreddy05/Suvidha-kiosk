package com.suvidha.billing.exception;

public class OtpAttemptsExceededException extends RuntimeException {
    private final String code = "OTP_ATTEMPTS_EXCEEDED";

    public OtpAttemptsExceededException(String message) {
        super(message);
    }

    public String getCode() {
        return code;
    }
}
