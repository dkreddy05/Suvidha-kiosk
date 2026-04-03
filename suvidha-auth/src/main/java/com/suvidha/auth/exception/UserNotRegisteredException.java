package com.suvidha.auth.exception;

public class UserNotRegisteredException extends ApiException {
    public UserNotRegisteredException(String message) {
        super("USER_NOT_REGISTERED", message);
    }
}
