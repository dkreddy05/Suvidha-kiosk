package com.suvidha.billing.service;

import com.suvidha.billing.dto.response.ServiceAccountResponse;
import java.util.List;

public interface ServiceAccountService {
    ServiceAccountResponse createFromVerification(String verificationRequestId);
    List<ServiceAccountResponse> getAccountsByCitizen(String citizenId);
    ServiceAccountResponse deactivateAccount(String accountId);
    ServiceAccountResponse getAccountDetails(String accountId);
}
