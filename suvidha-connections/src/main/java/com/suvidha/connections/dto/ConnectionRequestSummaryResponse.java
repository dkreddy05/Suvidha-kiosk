package com.suvidha.connections.dto;

import java.time.Instant;

public record ConnectionRequestSummaryResponse(
        String requestId,
        String serviceType,
        String address,
        String status,
        Instant submittedAt) {
}
