package com.suvidha.billing.service;

import com.suvidha.billing.dto.request.AccountVerificationRequestDto;
import com.suvidha.billing.dto.request.ConfirmLinkRequest;
import com.suvidha.billing.dto.request.VerifyOwnershipRequest;
import com.suvidha.billing.dto.response.AccountLinkHistoryResponse;
import com.suvidha.billing.dto.response.AccountLinkStatusResponse;
import com.suvidha.billing.dto.response.CancelResponse;

import java.util.List;

public interface AccountLinkingService {
    Object verifyOwnership(String citizenId, VerifyOwnershipRequest req);

    Object confirmLink(String citizenId, ConfirmLinkRequest req);

    Object requestVerification(String citizenId, AccountVerificationRequestDto req);

    Object getVerificationStatus(String refNo);

    Object getProviders(com.suvidha.billing.enums.ServiceType serviceType);

    AccountLinkStatusResponse trackStatus(String citizenId);

    List<AccountLinkHistoryResponse> getHistory(String citizenId);

    CancelResponse cancelLink(String requestId);
}
