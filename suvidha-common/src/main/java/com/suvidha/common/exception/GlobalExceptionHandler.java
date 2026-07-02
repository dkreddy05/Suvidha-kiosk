package com.suvidha.common.exception;

import com.suvidha.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler shared across all Suvidha services.
 * Provides consistent error response formatting.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                ex.getDetails().isEmpty() ? null : ex.getDetails());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, Object> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                Instant.now(),
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        String raw = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String msg = "Data integrity violation.";
        if (raw != null) {
            String lower = raw.toLowerCase();
            if (lower.contains("aadhar")) {
                msg = "User with this Aadhar number is already registered.";
            } else if (lower.contains("mobile")) {
                msg = "User with this mobile number is already registered.";
            }
        }
        ErrorResponse error = new ErrorResponse(
                "CONFLICT",
                msg,
                Instant.now(),
                request.getRequestURI(),
                null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_REQUEST",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI(),
                null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                Instant.now(),
                request.getRequestURI(),
                null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
