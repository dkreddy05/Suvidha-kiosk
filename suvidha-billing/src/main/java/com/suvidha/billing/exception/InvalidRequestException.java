package com.suvidha.billing.exception;

public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }
}
