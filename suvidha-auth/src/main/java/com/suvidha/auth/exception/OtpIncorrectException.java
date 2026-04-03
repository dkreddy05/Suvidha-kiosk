package com.suvidha.auth.exception;

public class OtpIncorrectException extends ApiException {
    private final int attemptsRemaining;

    public OtpIncorrectException(String message, int attemptsRemaining) {
        super("OTP_INCORRECT", message);
        this.attemptsRemaining = attemptsRemaining;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }
}
