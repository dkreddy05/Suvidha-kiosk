package com.suvidha.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.suvidha.billing.entity.NewConnectionRequest;
import java.util.List;
import java.util.Optional;

import com.suvidha.billing.enums.ConnectionStatus;
import com.suvidha.billing.enums.ServiceType;
import jakarta.persistence.LockModeType;

@Repository
public interface NewConnectionRequestRepository extends JpaRepository<NewConnectionRequest, String> {
    Optional<NewConnectionRequest> findByRefNo(String refNo);

    List<NewConnectionRequest> findByCitizenId(String citizenId);

    int countByCitizenIdAndStatus(String citizenId, com.suvidha.billing.enums.ConnectionStatus status);

    /**
     * Pessimistic-locking count query to prevent concurrent pending-request limit bypass.
     * Locks the matching rows so concurrent transactions serialize on this check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(n) FROM NewConnectionRequest n WHERE n.citizenId = :citizenId AND n.status = :status")
    int countByCitizenIdAndStatusWithLock(@Param("citizenId") String citizenId,
                                          @Param("status") ConnectionStatus status);

    Optional<NewConnectionRequest> findByCitizenIdAndServiceTypeAndStatusIn(
            String citizenId,
            ServiceType type,
            List<ConnectionStatus> statuses);

    List<NewConnectionRequest> findAllByCitizenId(String citizenId);
}
