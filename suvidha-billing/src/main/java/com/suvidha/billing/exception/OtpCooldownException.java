package com.suvidha.billing.exception;

public class OtpCooldownException extends ApiException {
    public OtpCooldownException(String message) {
        super("OTP_COOLDOWN", message);
    }
}
