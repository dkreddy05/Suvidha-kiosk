package com.suvidha.billing.exception;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
