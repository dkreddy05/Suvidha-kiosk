package com.suvidha.connections.dto;

import java.time.Instant;

public record ConnectionTimelineEntryResponse(
        String status,
        String message,
        Instant updatedAt) {
}
