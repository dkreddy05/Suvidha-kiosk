package com.suvidha.billing.exception;

public class InvalidConsumerIdException extends ApiException {
    public InvalidConsumerIdException(String message) {
        super("INVALID_CONSUMER_ID", message);
    }
}
