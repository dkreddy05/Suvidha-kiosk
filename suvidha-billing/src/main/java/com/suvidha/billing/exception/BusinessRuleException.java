package com.suvidha.billing.exception;

/**
 * Thrown when a business rule is violated (e.g. wrong state transition,
 * precondition failed). Mapped to HTTP 409 Conflict.
 */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String message) {
        super("BUSINESS_RULE_VIOLATION", message);
    }

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
