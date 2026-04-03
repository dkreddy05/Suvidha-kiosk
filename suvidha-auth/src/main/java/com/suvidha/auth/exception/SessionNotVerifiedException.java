package com.suvidha.auth.exception;

public class SessionNotVerifiedException extends ApiException {
    public SessionNotVerifiedException(String message) {
        super("SESSION_NOT_VERIFIED", message);
    }
}
