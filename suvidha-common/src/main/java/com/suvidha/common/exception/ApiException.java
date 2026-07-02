package com.suvidha.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Base exception for all Suvidha API errors.
 * Services should extend this or use it directly with an {@link ErrorCode}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? details : Collections.emptyMap();
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = Collections.emptyMap();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
