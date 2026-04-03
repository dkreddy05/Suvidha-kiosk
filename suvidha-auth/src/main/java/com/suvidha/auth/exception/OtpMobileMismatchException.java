package com.suvidha.auth.exception;

public class OtpMobileMismatchException extends ApiException {
    public OtpMobileMismatchException(String message) {
        super("OTP_MOBILE_MISMATCH", message);
    }
}
