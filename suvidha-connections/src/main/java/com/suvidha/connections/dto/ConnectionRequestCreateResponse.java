package com.suvidha.connections.dto;

public record ConnectionRequestCreateResponse(
        String requestId,
        String status,
        int estimatedDays) {
}
