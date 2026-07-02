package com.suvidha.billing.exception;

public abstract class ApiException extends RuntimeException {
    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
