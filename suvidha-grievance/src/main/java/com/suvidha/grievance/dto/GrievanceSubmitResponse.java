package com.suvidha.grievance.dto;

import java.time.Instant;

public record GrievanceSubmitResponse(
        String referenceNumber,
        Instant submittedAt,
        String status) {
}
