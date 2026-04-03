package com.suvidha.auth.exception;

public class OtpSessionInvalidException extends ApiException {
    public OtpSessionInvalidException(String message) {
        super("OTP_SESSION_INVALID", message);
    }
}
