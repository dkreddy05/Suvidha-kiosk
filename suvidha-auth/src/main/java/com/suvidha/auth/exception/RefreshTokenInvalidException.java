package com.suvidha.auth.exception;

public class RefreshTokenInvalidException extends ApiException {
    public RefreshTokenInvalidException(String message) {
        super("REFRESH_TOKEN_INVALID", message);
    }
}
