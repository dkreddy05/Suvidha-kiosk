package com.suvidha.grievance.dto;

import java.time.Instant;

public record GrievanceSummaryResponse(
        String referenceNumber,
        String category,
        String status,
        Instant submittedAt) {
}
