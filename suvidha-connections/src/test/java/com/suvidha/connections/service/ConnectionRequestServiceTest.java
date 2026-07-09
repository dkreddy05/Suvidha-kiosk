package com.suvidha.connections.service;

import com.suvidha.connections.dto.ConnectionRequestCreateRequest;
import com.suvidha.connections.dto.ConnectionDocumentRequest;
import com.suvidha.connections.dto.ConnectionStatusResponse;
import com.suvidha.connections.dto.ConnectionStatusUpdateRequest;
import com.suvidha.connections.model.ConnectionRequest;
import com.suvidha.connections.repository.ConnectionRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionRequestServiceTest {

    @Mock
    private ConnectionRequestRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ConnectionRequestService service;

    private static final String CITIZEN_A = "citizen-A";
    private static final String CITIZEN_B = "citizen-B";
    private static final String DISPLAY_ID = "CONN-10001";

    @BeforeEach
    void setUp() {
        service = new ConnectionRequestService(repository, eventPublisher);
    }

    private ConnectionRequest aSubmittedRequest() {
        return ConnectionRequest.builder()
                .id(UUID.randomUUID())
                .displayId(DISPLAY_ID)
                .citizenId(CITIZEN_A)
                .serviceType("WATER")
                .address("123 Main St")
                .status("SUBMITTED")
                .estimatedDays(7)
                .submittedAt(Instant.now())
                .timeline(new java.util.ArrayList<>())
                .documents(new java.util.ArrayList<>())
                .statusHistory(new java.util.ArrayList<>())
                .version(0L)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates a new connection request with next sequence")
        void createsWithNextSequence() {
            when(repository.nextDisplaySequence()).thenReturn(10001L);

            var req = new ConnectionRequestCreateRequest(
                    "WATER", "123 Main St",
                    List.of(new ConnectionDocumentRequest("AADHAAR", "doc-url")));
            var response = service.create(CITIZEN_A, req);

            assertEquals("CONN-10001", response.requestId());
            assertEquals("SUBMITTED", response.status());
            assertEquals(7, response.estimatedDays());

            ArgumentCaptor<ConnectionRequest> captor = ArgumentCaptor.forClass(ConnectionRequest.class);
            verify(repository).save(captor.capture());
            ConnectionRequest saved = captor.getValue();
            assertEquals("CONN-10001", saved.getDisplayId());
            assertEquals(CITIZEN_A, saved.getCitizenId());
            assertEquals(1, saved.getTimeline().size());
            assertEquals(1, saved.getDocuments().size());
            assertEquals(1, saved.getStatusHistory().size());

            verify(eventPublisher).publishEvent(any(ConnectionRequestService.ConnectionSubmittedEvent.class));
        }
    }

    @Nested
    @DisplayName("status() ownership checks")
    class StatusOwnership {

        @Test
        @DisplayName("owner can view their own connection request")
        void ownerCanViewOwnRequest() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));

            ConnectionStatusResponse response = service.status(DISPLAY_ID, CITIZEN_A);

            assertNotNull(response);
            assertEquals(DISPLAY_ID, response.requestId());
        }

        @Test
        @DisplayName("non-owner cannot view another citizen's connection request")
        void nonOwnerCannotViewOthersRequest() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.status(DISPLAY_ID, CITIZEN_B));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertTrue(ex.getReason().contains("permission"));
        }

        @Test
        @DisplayName("returns 404 for non-existent request before ownership check")
        void notFoundForNonExistentRequest() {
            when(repository.findByDisplayId("CONN-99999")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.status("CONN-99999", CITIZEN_A));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("updateStatus() ownership checks")
    class UpdateStatusOwnership {

        @Test
        @DisplayName("admin can move request to UNDER_REVIEW")
        void adminCanMoveToUnderReview() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            var update = new ConnectionStatusUpdateRequest("UNDER_REVIEW", "processing");
            ConnectionStatusResponse response = service.updateStatus(DISPLAY_ID, "admin-1", true, update);

            assertNotNull(response);
            assertEquals("UNDER_REVIEW", response.status());
        }

        @Test
        @DisplayName("citizen can CANCEL their own request")
        void citizenCanCancelOwnRequest() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            var update = new ConnectionStatusUpdateRequest("CANCELLED", "changed my mind");
            ConnectionStatusResponse response = service.updateStatus(DISPLAY_ID, CITIZEN_A, false, update);

            assertNotNull(response);
            assertEquals("CANCELLED", response.status());
        }

        @Test
        @DisplayName("citizen cannot make admin-only transitions (UNDER_REVIEW) — gets FORBIDDEN")
        void citizenCannotMakeAdminTransitions() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(DISPLAY_ID, CITIZEN_A, false,
                            new ConnectionStatusUpdateRequest("UNDER_REVIEW", null)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertTrue(ex.getReason().contains("admins"));
        }

        @Test
        @DisplayName("non-owner non-admin cannot update another citizen's connection request")
        void nonOwnerCannotUpdateOthersRequest() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(DISPLAY_ID, CITIZEN_B, false,
                            new ConnectionStatusUpdateRequest("UNDER_REVIEW", null)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertTrue(ex.getReason().contains("permission"));
        }

        @Test
        @DisplayName("non-owner update does not modify state")
        void nonOwnerUpdateDoesNotModifyState() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));

            assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(DISPLAY_ID, CITIZEN_B, false,
                            new ConnectionStatusUpdateRequest("UNDER_REVIEW", null)));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("returns 404 for non-existent request before ownership check")
        void notFoundForNonExistentRequest() {
            when(repository.findByDisplayId("CONN-99999")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus("CONN-99999", CITIZEN_A, false,
                            new ConnectionStatusUpdateRequest("UNDER_REVIEW", null)));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("myRequests() already filters by citizenId")
    class MyRequestsIsolation {

        @Test
        @DisplayName("citizen only sees their own requests")
        void citizenSeesOnlyOwnRequests() {
            ConnectionRequest reqA1 = aSubmittedRequest();
            ConnectionRequest reqA2 = aSubmittedRequest();
            ConnectionRequest reqB = aSubmittedRequest();
            reqB.setCitizenId(CITIZEN_B);

            when(repository.findByCitizenIdOrderBySubmittedAtDesc(CITIZEN_A))
                    .thenReturn(List.of(reqA1, reqA2));
            when(repository.findByCitizenIdOrderBySubmittedAtDesc(CITIZEN_B))
                    .thenReturn(List.of(reqB));

            var aRequests = service.myRequests(CITIZEN_A);
            var bRequests = service.myRequests(CITIZEN_B);

            assertEquals(2, aRequests.size());
            assertEquals(1, bRequests.size());
        }
    }

    @Nested
    @DisplayName("status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("admin: rejects invalid transition from SUBMITTED directly to APPROVED")
        void rejectsInvalidTransition() {
            ConnectionRequest req = aSubmittedRequest();
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));

            // Admin passes auth check; transition logic (SUBMITTED->APPROVED) is invalid
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(DISPLAY_ID, "admin-1", true,
                            new ConnectionStatusUpdateRequest("APPROVED", null)));

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        }

        @Test
        @DisplayName("admin: rejects update on terminal state APPROVED")
        void rejectsUpdateOnTerminalState() {
            ConnectionRequest req = aSubmittedRequest();
            req.setStatus("APPROVED");
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));

            // Admin passes auth check; terminal-state guard fires
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(DISPLAY_ID, "admin-1", true,
                            new ConnectionStatusUpdateRequest("UNDER_REVIEW", null)));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        @DisplayName("admin: publishes ConnectionApprovedEvent on approval")
        void publishesApprovedEvent() {
            ConnectionRequest req = aSubmittedRequest();
            req.setStatus("UNDER_REVIEW");
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.updateStatus(DISPLAY_ID, "admin-1", true,
                    new ConnectionStatusUpdateRequest("APPROVED", "Approved"));

            verify(eventPublisher).publishEvent(any(ConnectionRequestService.ConnectionApprovedEvent.class));
        }

        @Test
        @DisplayName("admin: publishes ConnectionRejectedEvent on rejection")
        void publishesRejectedEvent() {
            ConnectionRequest req = aSubmittedRequest();
            req.setStatus("UNDER_REVIEW");
            when(repository.findByDisplayId(DISPLAY_ID)).thenReturn(Optional.of(req));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.updateStatus(DISPLAY_ID, "admin-1", true,
                    new ConnectionStatusUpdateRequest("REJECTED", "Invalid docs"));

            verify(eventPublisher).publishEvent(any(ConnectionRequestService.ConnectionRejectedEvent.class));
        }
    }
}