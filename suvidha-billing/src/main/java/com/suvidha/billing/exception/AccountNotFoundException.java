package com.suvidha.billing.exception;

public class AccountNotFoundException extends ApiException {
    public AccountNotFoundException(String message) {
        super("ACCOUNT_NOT_FOUND", message);
    }
}
