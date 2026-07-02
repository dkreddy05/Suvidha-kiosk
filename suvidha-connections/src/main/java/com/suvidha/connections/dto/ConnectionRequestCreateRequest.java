package com.suvidha.connections.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConnectionRequestCreateRequest(
        @NotBlank(message = "serviceType is required") String serviceType,
        @NotBlank(message = "address is required") @Size(max = 512, message = "address too long") String address,
        @Valid @NotEmpty(message = "documents are required") List<ConnectionDocumentRequest> documents) {
}
