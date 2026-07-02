package com.suvidha.billing.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.suvidha.billing.entity.ServiceAccount;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import com.suvidha.billing.enums.ServiceType;

@Repository
public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, String> {
    Optional<ServiceAccount> findByAccountNo(String accountNo);

    Optional<ServiceAccount> findByAccountNoAndServiceType(String accountNo, ServiceType serviceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sa FROM ServiceAccount sa WHERE sa.accountNo = :accountNo AND sa.serviceType = :serviceType")
    Optional<ServiceAccount> findByAccountNoAndServiceTypeForUpdate(
            @Param("accountNo") String accountNo,
            @Param("serviceType") ServiceType serviceType);

    Optional<ServiceAccount> findByCitizenIdAndServiceType(String citizenId, ServiceType type);

    List<ServiceAccount> findByCitizenId(String citizenId);

    List<ServiceAccount> findByRegisteredMobileAndIsActive(String mobile, boolean isActive);
}
