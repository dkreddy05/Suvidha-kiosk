package com.suvidha.billing.exception;

public class LinkInitRateLimitExceededException extends ApiException {
    public LinkInitRateLimitExceededException(String message) {
        super("LINK_INIT_RATE_LIMIT", message);
    }
}
