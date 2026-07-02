package com.suvidha.billing.exception;

public class AccountLinkNotFoundException extends ApiException {
    public AccountLinkNotFoundException(String message) {
        super("ACCOUNT_LINK_NOT_FOUND", message);
    }
}
