package com.suvidha.connections.service;

import com.suvidha.connections.dto.ConnectionRequestCreateRequest;
import com.suvidha.connections.dto.ConnectionDocumentRequest;
import com.suvidha.connections.dto.ConnectionStatusResponse;
import com.suvidha.connections.dto.ConnectionStatusUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionRequestServiceTest {

    private ConnectionRequestService service;

    private static final String CITIZEN_A = "citizen-A";
    private static final String CITIZEN_B = "citizen-B";

    @BeforeEach
    void setUp() {
        service = new ConnectionRequestService();
    }

    private String createRequestFor(String citizenId) {
        var req = new ConnectionRequestCreateRequest(
                "WATER",
                "123 Main St",
                List.of(new ConnectionDocumentRequest("AADHAAR", "doc-url")));
        return service.create(citizenId, req).requestId();
    }

    @Nested
    @DisplayName("status() ownership checks")
    class StatusOwnership {

        @Test
        @DisplayName("owner can view their own connection request")
        void ownerCanViewOwnRequest() {
            String requestId = createRequestFor(CITIZEN_A);

            ConnectionStatusResponse response = service.status(requestId, CITIZEN_A);

            assertNotNull(response);
            assertEquals(requestId, response.requestId());
        }

        @Test
        @DisplayName("non-owner cannot view another citizen's connection request")
        void nonOwnerCannotViewOthersRequest() {
            String requestId = createRequestFor(CITIZEN_A);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.status(requestId, CITIZEN_B));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertTrue(ex.getReason().contains("permission"));
        }

        @Test
        @DisplayName("returns 404 for non-existent request before ownership check")
        void notFoundForNonExistentRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.status("CONN-99999", CITIZEN_A));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("updateStatus() ownership checks")
    class UpdateStatusOwnership {

        @Test
        @DisplayName("owner can update their own connection request")
        void ownerCanUpdateOwnRequest() {
            String requestId = createRequestFor(CITIZEN_A);
            var update = new ConnectionStatusUpdateRequest("UNDER_REVIEW", "processing");

            ConnectionStatusResponse response = service.updateStatus(requestId, CITIZEN_A, update);

            assertNotNull(response);
            assertEquals("UNDER_REVIEW", response.status());
        }

        @Test
        @DisplayName("non-owner cannot update another citizen's connection request")
        void nonOwnerCannotUpdateOthersRequest() {
            String requestId = createRequestFor(CITIZEN_A);
            var update = new ConnectionStatusUpdateRequest("UNDER_REVIEW", null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(requestId, CITIZEN_B, update));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertTrue(ex.getReason().contains("permission"));
        }

        @Test
        @DisplayName("non-owner update does not modify state")
        void nonOwnerUpdateDoesNotModifyState() {
            String requestId = createRequestFor(CITIZEN_A);

            assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus(requestId, CITIZEN_B,
                            new ConnectionStatusUpdateRequest("UNDER_REVIEW", null)));

            // Verify state unchanged — owner can still transition
            var update = new ConnectionStatusUpdateRequest("UNDER_REVIEW", null);
            ConnectionStatusResponse response = service.updateStatus(requestId, CITIZEN_A, update);
            assertEquals("UNDER_REVIEW", response.status());
        }

        @Test
        @DisplayName("returns 404 for non-existent request before ownership check")
        void notFoundForNonExistentRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateStatus("CONN-99999", CITIZEN_A,
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
            createRequestFor(CITIZEN_A);
            createRequestFor(CITIZEN_B);
            createRequestFor(CITIZEN_A);

            var aRequests = service.myRequests(CITIZEN_A);
            var bRequests = service.myRequests(CITIZEN_B);

            assertEquals(2, aRequests.size());
            assertEquals(1, bRequests.size());
        }
    }
}
