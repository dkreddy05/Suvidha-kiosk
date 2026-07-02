package com.suvidha.billing.exception;

public class PaymentAmountMismatchException extends ApiException {
    public PaymentAmountMismatchException(String message) {
        super("AMOUNT_MISMATCH", message);
    }
}
