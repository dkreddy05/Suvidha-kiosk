package com.suvidha.auth.exception;

public class UserAlreadyExistsException extends ApiException {
    public UserAlreadyExistsException(String message) {
        super("USER_ALREADY_EXISTS", message);
    }
}
