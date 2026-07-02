package com.suvidha.billing.service;

import com.suvidha.billing.dto.response.ServiceAccountResponse;
import com.suvidha.billing.entity.AccountVerificationRequest;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.enums.VerificationStatus;
import com.suvidha.billing.exception.AccountNotFoundException;
import com.suvidha.billing.exception.BusinessRuleException;
import com.suvidha.billing.repository.AccountVerificationRequestRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class ServiceAccountServiceImpl implements ServiceAccountService {

    private final ServiceAccountRepository serviceAccountRepository;
    private final AccountVerificationRequestRepository verificationRepo;

    public ServiceAccountServiceImpl(ServiceAccountRepository serviceAccountRepository,
            AccountVerificationRequestRepository verificationRepo) {
        this.serviceAccountRepository = serviceAccountRepository;
        this.verificationRepo = verificationRepo;
    }

    @Override
    @Transactional
    public ServiceAccountResponse createFromVerification(String verificationRequestId) {
        AccountVerificationRequest request = verificationRepo.findById(verificationRequestId)
                .orElseThrow(() -> new AccountNotFoundException("Verification request not found"));
        if (request.getStatus() != VerificationStatus.APPROVED) {
            throw new BusinessRuleException("INVALID_STATE", "Verification request is not approved");
        }
        ServiceType mappedServiceType = ServiceType.WATER;
        if (request.getProviderName().toLowerCase().contains("electricity")) {
            mappedServiceType = ServiceType.ELECTRICITY;
        }
        Optional<ServiceAccount> existing = serviceAccountRepository
                .findByAccountNoAndServiceType(request.getConsumerNo(), mappedServiceType);

        if (existing.isPresent()) {
            throw new BusinessRuleException("DUPLICATE_ACCOUNT", "Service account already exists");
        }
        ServiceAccount account = ServiceAccount.builder()
                .citizenId(request.getCitizenId())
                .serviceType(mappedServiceType)
                .accountNo(request.getConsumerNo())
                .providerName(request.getProviderName())
                .address(request.getAddress())
                .registeredMobile(request.getRegisteredMobile())
                .isActive(true)
                .bills(new ArrayList<>())
                .build();
        ServiceAccount saved = serviceAccountRepository.save(account);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceAccountResponse> getAccountsByCitizen(String citizenId) {
        return serviceAccountRepository.findByCitizenId(citizenId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ServiceAccountResponse deactivateAccount(String accountId) {
        ServiceAccount account = serviceAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Service account not found"));
        account.setActive(false);
        return mapToResponse(serviceAccountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceAccountResponse getAccountDetails(String accountId) {
        ServiceAccount account = serviceAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Service account not found"));
        return mapToResponse(account);
    }

    private ServiceAccountResponse mapToResponse(ServiceAccount account) {
        return ServiceAccountResponse.builder()
                .id(account.getId())
                .citizenId(account.getCitizenId())
                .serviceType(account.getServiceType())
                .accountNo(account.getAccountNo())
                .providerName(account.getProviderName())
                .address(account.getAddress())
                .registeredMobile(account.getRegisteredMobile())
                .isActive(account.isActive())
                .build();
    }
}
