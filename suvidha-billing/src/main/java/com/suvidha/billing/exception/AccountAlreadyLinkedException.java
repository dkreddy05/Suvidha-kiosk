package com.suvidha.billing.exception;

public class AccountAlreadyLinkedException extends ApiException {
    public AccountAlreadyLinkedException(String message) {
        super("ACCOUNT_ALREADY_LINKED", message);
    }
}
