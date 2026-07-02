package com.suvidha.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.suvidha.billing.entity.AccountVerificationRequest;
import java.util.Optional;

import com.suvidha.billing.enums.VerificationStatus;

@Repository
public interface AccountVerificationRequestRepository extends JpaRepository<AccountVerificationRequest, String> {
    Optional<AccountVerificationRequest> findByRefNo(String refNo);

    Optional<AccountVerificationRequest> findByConsumerNoAndStatus(String consumerNo, VerificationStatus status);

    boolean existsByRefNo(String refNo);
}
