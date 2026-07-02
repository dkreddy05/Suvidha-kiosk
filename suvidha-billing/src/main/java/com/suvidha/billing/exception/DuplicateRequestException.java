package com.suvidha.billing.exception;

public class DuplicateRequestException extends ApiException {
    public DuplicateRequestException(String message) {
        super("REQUEST_ALREADY_EXISTS", message);
    }
}
