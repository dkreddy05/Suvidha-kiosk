package com.suvidha.billing.service;

import com.suvidha.billing.dto.response.NewConnectionResponse;
import com.suvidha.billing.dto.response.ServiceAccountResponse;
import com.suvidha.billing.entity.NewConnectionRequest;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.enums.ConnectionStatus;
import com.suvidha.billing.exception.AccountNotFoundException;
import com.suvidha.billing.exception.BusinessRuleException;
import com.suvidha.billing.repository.NewConnectionRequestRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.suvidha.billing.dto.request.NewConnectionRequestDto;
import com.suvidha.billing.dto.response.ConnectionStatusResponse;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class NewConnectionServiceImpl implements NewConnectionService {

    private final NewConnectionRequestRepository newConnectionRepo;

    private final ServiceAccountRepository serviceAccountRepository;

    private final java.security.SecureRandom secureRandom;

    public NewConnectionServiceImpl(NewConnectionRequestRepository newConnectionRepo,
            ServiceAccountRepository serviceAccountRepository,
            java.security.SecureRandom secureRandom) {
        this.newConnectionRepo = newConnectionRepo;
        this.serviceAccountRepository = serviceAccountRepository;
        this.secureRandom = secureRandom;
    }

    @Override
    @Transactional
    public NewConnectionResponse submitRequest(String citizenId, NewConnectionRequestDto dto) {

        int pendingCount = newConnectionRepo.countByCitizenIdAndStatusWithLock(citizenId, ConnectionStatus.PENDING);
        if (pendingCount >= 3) {
            throw new BusinessRuleException("PENDING_LIMIT_EXCEEDED", "Maximum 3 pending requests allowed");
        }

        String refNo = "CONN-" + dto.getServiceType().name() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        NewConnectionRequest request = NewConnectionRequest.builder()
                .citizenId(citizenId)
                .serviceType(dto.getServiceType())
                .address(dto.getAddress())
                .propertyType(dto.getPropertyType())
                .status(ConnectionStatus.PENDING)
                .refNo(refNo)
                .build();

        NewConnectionRequest saved = newConnectionRepo.save(request);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NewConnectionResponse getStatus(String refNo) {
        NewConnectionRequest req = newConnectionRepo.findByRefNo(refNo)
                .orElseThrow(() -> new AccountNotFoundException("Request not found with ref: " + refNo));
        return mapToResponse(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewConnectionResponse> getRequestsByCitizen(String citizenId) {
        return newConnectionRepo.findByCitizenId(citizenId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NewConnectionResponse approveRequest(String requestId, String providerName) {
        NewConnectionRequest req = newConnectionRepo.findById(requestId)
                .orElseThrow(() -> new AccountNotFoundException("Request not found"));

        if (req.getStatus() != ConnectionStatus.PENDING) {
            throw new BusinessRuleException("INVALID_STATE", "Can only approve PENDING requests");
        }

        req.setStatus(ConnectionStatus.APPROVED);
        req.setProviderName(providerName);
        return mapToResponse(newConnectionRepo.save(req));
    }

    @Override
    @Transactional
    public NewConnectionResponse rejectRequest(String requestId, String reason) {
        NewConnectionRequest req = newConnectionRepo.findById(requestId)
                .orElseThrow(() -> new AccountNotFoundException("Request not found"));

        if (req.getStatus() != ConnectionStatus.PENDING) {
            throw new BusinessRuleException("INVALID_STATE", "Can only reject PENDING requests");
        }

        req.setStatus(ConnectionStatus.REJECTED);
        return mapToResponse(newConnectionRepo.save(req));
    }

    private String generateConsumerId(com.suvidha.billing.enums.ServiceType serviceType) {
        String prefix = switch (serviceType) {
            case ELECTRICITY -> "E";
            case WATER -> "W";
            case GAS -> "G";
        };
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 9; i++) {
                sb.append(secureRandom.nextInt(10));
            }
            String accountNo = sb.toString();
            if (serviceAccountRepository.findByAccountNoAndServiceType(accountNo, serviceType).isEmpty()) {
                return accountNo;
            }
        }
        throw new IllegalStateException("Failed to generate unique consumer ID");
    }

    @Override
    @Transactional
    public ServiceAccountResponse completeConnection(String requestId, String accountNo) {
        NewConnectionRequest req = newConnectionRepo.findById(requestId)
                .orElseThrow(() -> new AccountNotFoundException("Request not found"));

        if (req.getStatus() != ConnectionStatus.APPROVED
                && req.getStatus() != ConnectionStatus.IN_PROGRESS) {
            throw new BusinessRuleException("INVALID_STATE", "Request must be APPROVED to complete");
        }

        String finalAccountNo = accountNo;
        if (finalAccountNo == null || finalAccountNo.isBlank() || "AUTO".equalsIgnoreCase(finalAccountNo)) {
            finalAccountNo = generateConsumerId(req.getServiceType());
        }

        ServiceAccount account = ServiceAccount.builder()
                .citizenId(req.getCitizenId())
                .serviceType(req.getServiceType())
                .accountNo(finalAccountNo)
                .providerName(req.getProviderName())
                .address(req.getAddress())
                .registeredMobile("NOT_FURNISHED")
                .bills(new ArrayList<>())
                .build();
        ServiceAccount savedAccount = serviceAccountRepository.save(account);
        req.setStatus(ConnectionStatus.COMPLETED);
        newConnectionRepo.save(req);
        return ServiceAccountResponse.builder()
                .id(savedAccount.getId())
                .citizenId(savedAccount.getCitizenId())
                .serviceType(savedAccount.getServiceType())
                .accountNo(savedAccount.getAccountNo())
                .providerName(savedAccount.getProviderName())
                .address(savedAccount.getAddress())
                .registeredMobile(savedAccount.getRegisteredMobile())
                .isActive(savedAccount.isActive())
                .build();
    }

    private NewConnectionResponse mapToResponse(NewConnectionRequest req) {
        return NewConnectionResponse.builder()
                .id(req.getId())
                .citizenId(req.getCitizenId())
                .serviceType(req.getServiceType())
                .address(req.getAddress())
                .propertyType(req.getPropertyType())
                .providerName(req.getProviderName())
                .status(req.getStatus())
                .refNo(req.getRefNo())
                .build();
    }

    @Override
    public ConnectionStatusResponse applyForConnection(String citizenId, NewConnectionRequestDto req) {
        NewConnectionResponse res = submitRequest(citizenId, req);
        return new ConnectionStatusResponse(res.getRefNo(), res.getStatus(), res.getServiceType());
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectionStatusResponse trackConnection(String refNo) {
        NewConnectionResponse res = getStatus(refNo);
        return new ConnectionStatusResponse(res.getRefNo(), res.getStatus(), res.getServiceType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionStatusResponse> myConnections(String citizenId) {
        return getRequestsByCitizen(citizenId).stream()
                .map(res -> new ConnectionStatusResponse(res.getRefNo(), res.getStatus(), res.getServiceType()))
                .collect(Collectors.toList());
    }
}
