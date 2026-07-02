package com.suvidha.billing.controller;

import com.suvidha.billing.dto.response.NewConnectionResponse;
import com.suvidha.billing.enums.ConnectionStatus;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.exception.UnauthorizedException;
import com.suvidha.billing.security.CitizenAuthDetails;
import com.suvidha.billing.service.NewConnectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewConnectionControllerTest {

    @Mock
    private NewConnectionService newConnectionService;

    private NewConnectionController controller;

    @BeforeEach
    void setUp() {
        controller = new NewConnectionController(newConnectionService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getByCitizen_returnsConnections_whenCitizenIdMatches() {
        setSecurityContext("citizen-1");

        NewConnectionResponse resp = NewConnectionResponse.builder()
                .id("req-1")
                .citizenId("citizen-1")
                .serviceType(ServiceType.ELECTRICITY)
                .status(ConnectionStatus.PENDING)
                .refNo("CONN-ELEC-ABC12345")
                .build();

        when(newConnectionService.getRequestsByCitizen("citizen-1")).thenReturn(List.of(resp));

        ResponseEntity<List<NewConnectionResponse>> result = controller.getByCitizen("citizen-1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).getCitizenId()).isEqualTo("citizen-1");
        verify(newConnectionService).getRequestsByCitizen("citizen-1");
    }

    @Test
    void getByCitizen_throwsUnauthorized_whenCitizenIdDoesNotMatch() {
        setSecurityContext("citizen-1");

        assertThatThrownBy(() -> controller.getByCitizen("citizen-2"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("cannot view connections for another citizen");

        verifyNoInteractions(newConnectionService);
    }

    @Test
    void getByCitizen_throwsUnauthorized_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> controller.getByCitizen("citizen-1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing citizenId");

        verifyNoInteractions(newConnectionService);
    }

    private void setSecurityContext(String citizenId) {
        CitizenAuthDetails details = new CitizenAuthDetails("9876543210");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(citizenId, null, List.of());
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
