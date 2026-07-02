package com.suvidha.grievance.dto;

import java.time.Instant;
import java.util.List;

public record GrievanceTrackResponse(
        String referenceNumber,
        String category,
        String description,
        String photoUrl,
        String status,
        List<StatusHistoryEntry> statusHistory,
        Instant submittedAt,
        Instant updatedAt) {

    public record StatusHistoryEntry(String status, Instant timestamp, String notes) {}
}
