package com.suvidha.auth.exception;

import com.suvidha.auth.Dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private String requestId(HttpServletRequest request) {
        String rid = request != null ? request.getHeader(REQUEST_ID_HEADER) : null;
        return rid != null && !rid.isBlank() ? rid : "";
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(SessionNotVerifiedException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotVerified(SessionNotVerifiedException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(OtpRateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpRateLimit(OtpRateLimitExceededException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(OtpSessionInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpSessionInvalid(OtpSessionInvalidException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(OtpMobileMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpMobileMismatch(OtpMobileMismatchException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(OtpMaxAttemptsExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpMaxAttempts(OtpMaxAttemptsExceededException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(OtpIncorrectException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpIncorrect(OtpIncorrectException ex, HttpServletRequest request) {
        String rid = requestId(request);
        // Keep the payload shape consistent with the kiosk frontend error parser.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(UserNotRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotRegistered(UserNotRegisteredException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(OtpSendFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpSendFailed(OtpSendFailedException ex, HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(OtpVerifyFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpVerifyFailed(OtpVerifyFailedException ex,
            HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), rid));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        String rid = requestId(request);

        String raw = null;
        if (ex.getMostSpecificCause() != null) {
            raw = ex.getMostSpecificCause().getMessage();
        }
        if (raw == null) {
            raw = ex.getMessage();
        }

        String msg = "User already exists.";
        String lower = raw != null ? raw.toLowerCase() : "";
        if (lower.contains("aadhar")) {
            msg = "User with this Aadhar number is already registered.";
        } else if (lower.contains("mobile")) {
            msg = "User with this mobile number is already registered.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of("USER_ALREADY_EXISTS", msg, rid));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String rid = requestId(request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(REQUEST_ID_HEADER, rid)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "Unexpected error.", rid));
    }
}
