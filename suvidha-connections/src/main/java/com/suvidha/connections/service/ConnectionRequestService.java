package com.suvidha.connections.service;

import com.suvidha.connections.dto.*;
import com.suvidha.connections.model.*;
import com.suvidha.connections.repository.ConnectionRequestRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@Transactional
public class ConnectionRequestService {

    private static final Set<String> VALID_TRANSITIONS = Set.of(
            "SUBMITTED->UNDER_REVIEW",
            "SUBMITTED->CANCELLED",
            "UNDER_REVIEW->APPROVED",
            "UNDER_REVIEW->REJECTED",
            "UNDER_REVIEW->CANCELLED");

    private static final Set<String> TERMINAL_STATES = Set.of("APPROVED", "REJECTED", "CANCELLED");

    private final ConnectionRequestRepository connectionRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ConnectionRequestService(ConnectionRequestRepository connectionRequestRepository,
                                     ApplicationEventPublisher eventPublisher) {
        this.connectionRequestRepository = connectionRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    public ConnectionRequestCreateResponse create(String citizenId, ConnectionRequestCreateRequest request) {
        UUID connectionId = UUID.randomUUID();
        long seq = connectionRequestRepository.nextDisplaySequence();
        String displayId = "CONN-" + seq;

        ConnectionRequest connRequest = ConnectionRequest.builder()
                .id(connectionId)
                .displayId(displayId)
                .citizenId(citizenId)
                .serviceType(request.serviceType())
                .address(request.address())
                .status("SUBMITTED")
                .estimatedDays(7)
                .timeline(new ArrayList<>())
                .documents(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        ConnectionTimeline initialTimeline = ConnectionTimeline.builder()
                .id(UUID.randomUUID())
                .connection(connRequest)
                .status("SUBMITTED")
                .message("Connection request submitted")
                .build();
        connRequest.getTimeline().add(initialTimeline);

        if (request.documents() != null) {
            for (ConnectionDocumentRequest docReq : request.documents()) {
                ConnectionDocument doc = ConnectionDocument.builder()
                        .id(UUID.randomUUID())
                        .connection(connRequest)
                        .type(docReq.type())
                        .base64(docReq.base64())
                        .build();
                connRequest.getDocuments().add(doc);
            }
        }

        ConnectionStatusHistory history = ConnectionStatusHistory.builder()
                .id(UUID.randomUUID())
                .connection(connRequest)
                .fromStatus(null)
                .toStatus("SUBMITTED")
                .comment("Initial submission")
                .build();
        connRequest.getStatusHistory().add(history);

        connectionRequestRepository.save(connRequest);

        eventPublisher.publishEvent(new ConnectionSubmittedEvent(this, displayId, citizenId, request.serviceType()));

        return new ConnectionRequestCreateResponse(displayId, connRequest.getStatus(), connRequest.getEstimatedDays());
    }

    @Transactional(readOnly = true)
    public List<ConnectionRequestSummaryResponse> myRequests(String citizenId) {
        List<ConnectionRequest> reqs = connectionRequestRepository.findByCitizenIdOrderBySubmittedAtDesc(citizenId);
        return reqs.stream()
                .map(r -> new ConnectionRequestSummaryResponse(
                        r.getDisplayId(),
                        r.getServiceType(),
                        r.getAddress(),
                        r.getStatus(),
                        r.getSubmittedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConnectionStatusResponse status(String displayId, String citizenId) {
        ConnectionRequest r = connectionRequestRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection request not found"));

        if (!r.getCitizenId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to view this connection request");
        }

        List<ConnectionTimelineEntryResponse> mappedTimeline = r.getTimeline().stream()
                .map(t -> new ConnectionTimelineEntryResponse(t.getStatus(), t.getMessage(), t.getCreatedAt()))
                .toList();

        return new ConnectionStatusResponse(
                r.getDisplayId(),
                r.getServiceType(),
                r.getAddress(),
                r.getStatus(),
                mappedTimeline);
    }

    public ConnectionStatusResponse updateStatus(String displayId, String callerId, boolean isAdmin,
                                                   ConnectionStatusUpdateRequest update) {
        ConnectionRequest r = connectionRequestRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection request not found"));

        boolean isOwner = r.getCitizenId().equals(callerId);
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to modify this connection request");
        }
        if (isOwner && !isAdmin && !"CANCELLED".equals(update.status().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only admins can approve or reject requests");
        }

        String currentStatus = r.getStatus();
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

        String message = update.comment() != null ? update.comment() : "Status updated to " + newStatus;

        ConnectionTimeline newTimeline = ConnectionTimeline.builder()
                .id(UUID.randomUUID())
                .connection(r)
                .status(newStatus)
                .message(message)
                .build();
        r.getTimeline().add(newTimeline);

        ConnectionStatusHistory history = ConnectionStatusHistory.builder()
                .id(UUID.randomUUID())
                .connection(r)
                .fromStatus(currentStatus)
                .toStatus(newStatus)
                .comment(update.comment())
                .build();
        r.getStatusHistory().add(history);

        r.setStatus(newStatus);
        connectionRequestRepository.save(r);

        switch (newStatus) {
            case "APPROVED" -> eventPublisher.publishEvent(new ConnectionApprovedEvent(this, displayId, callerId));
            case "REJECTED" -> eventPublisher.publishEvent(new ConnectionRejectedEvent(this, displayId, callerId, update.comment()));
        }

        List<ConnectionTimelineEntryResponse> mappedTimeline = r.getTimeline().stream()
                .map(t -> new ConnectionTimelineEntryResponse(t.getStatus(), t.getMessage(), t.getCreatedAt()))
                .toList();

        return new ConnectionStatusResponse(
                displayId,
                r.getServiceType(),
                r.getAddress(),
                newStatus,
                mappedTimeline);
    }

    // ── Event classes ──────────────────────────────────────────────────────

    public record ConnectionSubmittedEvent(Object source, String displayId, String citizenId, String serviceType) {}
    public record ConnectionApprovedEvent(Object source, String displayId, String citizenId) {}
    public record ConnectionRejectedEvent(Object source, String displayId, String citizenId, String reason) {}
}