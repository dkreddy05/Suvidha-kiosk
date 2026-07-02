package com.suvidha.auth.exception;

import com.suvidha.auth.Dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("DataIntegrityViolation returns generic message regardless of constraint type")
    void handleDataIntegrityViolation_returnsGenericMessage() {
        DataIntegrityViolationException aadharEx = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [uk_citizens_aadhar]");

        when(request.getHeader("X-Request-Id")).thenReturn(null);

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(aadharEx, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError().getCode()).isEqualTo("USER_ALREADY_EXISTS");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Registration failed.");
    }

    @Test
    @DisplayName("DataIntegrityViolation does not leak Aadhar in error message")
    void handleDataIntegrityViolation_doesNotLeakAadhar() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_citizens_aadhar\"");

        when(request.getHeader("X-Request-Id")).thenReturn(null);

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        String msg = response.getBody().getError().getMessage().toLowerCase();
        assertThat(msg).doesNotContain("aadhar");
        assertThat(msg).doesNotContain("mobile");
    }

    @Test
    @DisplayName("DataIntegrityViolation does not leak mobile in error message")
    void handleDataIntegrityViolation_doesNotLeakMobile() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_citizens_mobile\"");

        when(request.getHeader("X-Request-Id")).thenReturn(null);

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        String msg = response.getBody().getError().getMessage().toLowerCase();
        assertThat(msg).doesNotContain("aadhar");
        assertThat(msg).doesNotContain("mobile");
    }

    @Test
    @DisplayName("DataIntegrityViolation preserves request ID in response")
    void handleDataIntegrityViolation_preservesRequestId() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation");

        when(request.getHeader("X-Request-Id")).thenReturn("req-abc-123");

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("req-abc-123");
        assertThat(response.getBody().getError().getRequestId()).isEqualTo("req-abc-123");
    }
}
