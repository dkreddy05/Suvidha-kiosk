package com.suvidha.auth.exception;

public class OtpSendFailedException extends ApiException {
    public OtpSendFailedException(String message) {
        super("OTP_SEND_FAILED", message);
    }

    public OtpSendFailedException(String message, Throwable cause) {
        super("OTP_SEND_FAILED", message, cause);
    }
}
