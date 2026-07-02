package com.suvidha.billing.service;

import com.suvidha.billing.dto.request.NewConnectionRequestDto;
import com.suvidha.billing.dto.response.ConnectionStatusResponse;
import com.suvidha.billing.dto.response.NewConnectionResponse;
import com.suvidha.billing.dto.response.ServiceAccountResponse;
import java.util.List;

public interface NewConnectionService {
    NewConnectionResponse submitRequest(String citizenId, NewConnectionRequestDto dto);

    NewConnectionResponse getStatus(String refNo);

    List<NewConnectionResponse> getRequestsByCitizen(String citizenId);

    NewConnectionResponse approveRequest(String requestId, String providerName);

    NewConnectionResponse rejectRequest(String requestId, String reason);

    ServiceAccountResponse completeConnection(String requestId, String accountNo);

    ConnectionStatusResponse applyForConnection(String citizenId, NewConnectionRequestDto req);

    ConnectionStatusResponse trackConnection(String refNo);

    List<ConnectionStatusResponse> myConnections(String citizenId);
}
