package com.suvidha.connections.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnectionDocumentRequest(
        @NotBlank(message = "document type is required") @Size(max = 64, message = "document type too long") String type,
        @NotBlank(message = "document base64 is required") @Size(max = 5242880, message = "document too large (max 5MB base64)") String base64) {
}
