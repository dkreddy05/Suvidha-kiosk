package com.suvidha.connections.dto;

import java.util.List;

public record ConnectionStatusResponse(
        String requestId,
        String serviceType,
        String address,
        String status,
        List<ConnectionTimelineEntryResponse> timeline) {
}
