package com.suvidha.common.exception;

/**
 * Centralized error codes for all Suvidha services.
 * Each code maps to an HTTP status and a machine-readable identifier.
 */
public enum ErrorCode {

    // ── Auth Errors ──────────────────────────────────────────
    INVALID_OTP("AUTH_001", 400),
    OTP_EXPIRED("AUTH_002", 400),
    RATE_LIMIT_EXCEEDED("AUTH_003", 429),
    SESSION_NOT_VERIFIED("AUTH_004", 403),
    USER_ALREADY_EXISTS("AUTH_005", 409),
    USER_NOT_REGISTERED("AUTH_006", 404),
    OTP_MAX_ATTEMPTS("AUTH_007", 401),

    // ── Billing Errors ───────────────────────────────────────
    BILL_NOT_FOUND("BILL_001", 404),
    PAYMENT_FAILED("BILL_002", 500),
    ACCOUNT_NOT_FOUND("BILL_003", 404),

    // ── General Errors ───────────────────────────────────────
    INVALID_REQUEST("GEN_001", 400),
    VALIDATION_ERROR("GEN_002", 400),
    UNAUTHORIZED("GEN_003", 401),
    FORBIDDEN("GEN_004", 403),
    NOT_FOUND("GEN_005", 404),
    CONFLICT("GEN_006", 409),
    INTERNAL_ERROR("GEN_007", 500);

    private final String code;
    private final int httpStatus;

    ErrorCode(String code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
