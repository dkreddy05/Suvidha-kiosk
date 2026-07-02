package com.suvidha.connections.service;

import com.suvidha.connections.dto.ConnectionRequestCreateRequest;
import com.suvidha.connections.dto.ConnectionRequestCreateResponse;
import com.suvidha.connections.dto.ConnectionRequestSummaryResponse;
import com.suvidha.connections.dto.ConnectionStatusResponse;
import com.suvidha.connections.dto.ConnectionStatusUpdateRequest;
import com.suvidha.connections.dto.ConnectionTimelineEntryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ConnectionRequestService {

    private static final Set<String> VALID_TRANSITIONS = Set.of(
            "SUBMITTED->UNDER_REVIEW",
            "UNDER_REVIEW->APPROVED",
            "UNDER_REVIEW->REJECTED");

    private static final Set<String> TERMINAL_STATES = Set.of("APPROVED", "REJECTED");

    private final AtomicInteger sequence = new AtomicInteger(5000);
    private final Map<String, StoredConnectionRequest> requests = new ConcurrentHashMap<>();

    public ConnectionRequestCreateResponse create(String citizenId, ConnectionRequestCreateRequest request) {
        Instant now = Instant.now();
        String requestId = "CONN-" + sequence.incrementAndGet();
        StoredConnectionRequest stored = new StoredConnectionRequest(
                requestId,
                citizenId,
                request.serviceType(),
                request.address(),
                "SUBMITTED",
                7,
                now,
                List.of(new ConnectionTimelineEntryResponse("SUBMITTED", "Connection request submitted", now)));
        requests.put(requestId, stored);
        return new ConnectionRequestCreateResponse(requestId, stored.status(), stored.estimatedDays());
    }

    public List<ConnectionRequestSummaryResponse> myRequests(String citizenId) {
        return requests.values().stream()
                .filter(request -> request.citizenId().equals(citizenId))
                .sorted(Comparator.comparing(StoredConnectionRequest::submittedAt).reversed())
                .map(request -> new ConnectionRequestSummaryResponse(
                        request.requestId(),
                        request.serviceType(),
                        request.address(),
                        request.status(),
                        request.submittedAt()))
                .toList();
    }

    public ConnectionStatusResponse status(String requestId, String citizenId) {
        StoredConnectionRequest request = requests.get(requestId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection request not found");
        }
        if (!request.citizenId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to view this connection request");
        }
        return new ConnectionStatusResponse(
                request.requestId(),
                request.serviceType(),
                request.address(),
                request.status(),
                request.timeline());
    }

    public ConnectionStatusResponse updateStatus(String requestId, String citizenId, ConnectionStatusUpdateRequest update) {
        StoredConnectionRequest request = requests.get(requestId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection request not found");
        }
        if (!request.citizenId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to modify this connection request");
        }

        String currentStatus = request.status();
        String newStatus = update.status().toUpperCase();

        if (TERMINAL_STATES.contains(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Request is already in terminal state: " + currentStatus);
        }

        String transition = currentStatus + "->" + newStatus;
        if (!VALID_TRANSITIONS.contains(transition)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        Instant now = Instant.now();
        String message = update.comment() != null ? update.comment() : "Status updated to " + newStatus;
        List<ConnectionTimelineEntryResponse> updatedTimeline = new java.util.ArrayList<>(request.timeline());
        updatedTimeline.add(new ConnectionTimelineEntryResponse(newStatus, message, now));

        StoredConnectionRequest updated = new StoredConnectionRequest(
                request.requestId(),
                request.citizenId(),
                request.serviceType(),
                request.address(),
                newStatus,
                request.estimatedDays(),
                request.submittedAt(),
                List.copyOf(updatedTimeline));
        requests.put(requestId, updated);

        return new ConnectionStatusResponse(
                requestId,
                request.serviceType(),
                request.address(),
                newStatus,
                updated.timeline());
    }

    private record StoredConnectionRequest(
            String requestId,
            String citizenId,
            String serviceType,
            String address,
            String status,
            int estimatedDays,
            Instant submittedAt,
            List<ConnectionTimelineEntryResponse> timeline) {
    }
}
