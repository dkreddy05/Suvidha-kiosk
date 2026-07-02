package com.suvidha.billing.exception;

public class AccountLinkConflictException extends ApiException {
    public AccountLinkConflictException(String message) {
        super("ACCOUNT_LINK_CONFLICT", message);
    }
}
