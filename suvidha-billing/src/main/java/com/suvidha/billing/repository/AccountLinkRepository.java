package com.suvidha.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.suvidha.billing.entity.AccountLinkRequest;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.suvidha.billing.enums.LinkRequestStatus;
import com.suvidha.billing.enums.UtilityType;

@Repository
public interface AccountLinkRepository extends JpaRepository<AccountLinkRequest, UUID> {
        Optional<AccountLinkRequest> findByIdAndCitizenId(UUID id, String citizenId);

        Optional<AccountLinkRequest> findFirstByCitizenIdAndAccountNoAndUtilityTypeAndStatusOrderByCreatedAtDesc(
                        String citizenId,
                        String accountNo,
                        UtilityType utilityType,
                        LinkRequestStatus status);

        Optional<AccountLinkRequest> findFirstByAccountNoAndUtilityTypeAndStatusOrderByCreatedAtDesc(
                        String accountNo,
                        UtilityType utilityType,
                        LinkRequestStatus status);

        Optional<AccountLinkRequest> findFirstByCitizenIdOrderByCreatedAtDesc(String citizenId);

        List<AccountLinkRequest> findByMobileAndStatusIn(String mobile, List<LinkRequestStatus> statuses);

        Optional<AccountLinkRequest> findTopByAccountNoOrderByCreatedAtDesc(String accountNo);

        List<AccountLinkRequest> findByCitizenId(String citizenId);
}
