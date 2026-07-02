package com.suvidha.connections.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnectionStatusUpdateRequest(
        @NotBlank(message = "status is required") @Size(max = 32, message = "status too long") String status,
        @Size(max = 1024, message = "comment too long") String comment) {
}
