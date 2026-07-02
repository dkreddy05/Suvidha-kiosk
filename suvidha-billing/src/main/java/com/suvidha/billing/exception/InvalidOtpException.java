package com.suvidha.billing.exception;

public class InvalidOtpException extends ApiException {
    public InvalidOtpException(String message) {
        super("INVALID_OTP", message);
    }
}
