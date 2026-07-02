package com.suvidha.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Standard error response body returned by all Suvidha services.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String errorCode,
        String message,
        Instant timestamp,
        String path,
        Map<String, Object> details) {
}
