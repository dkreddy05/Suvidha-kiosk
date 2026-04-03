package com.suvidha.auth.exception;

public class OtpVerifyFailedException extends ApiException {
    public OtpVerifyFailedException(String message) {
        super("OTP_VERIFY_FAILED", message);
    }
}
