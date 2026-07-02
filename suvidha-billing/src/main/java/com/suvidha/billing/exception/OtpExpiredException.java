package com.suvidha.billing.exception;

public class OtpExpiredException extends ApiException {
    public OtpExpiredException(String message) {
        super("OTP_EXPIRED", message);
    }
}
