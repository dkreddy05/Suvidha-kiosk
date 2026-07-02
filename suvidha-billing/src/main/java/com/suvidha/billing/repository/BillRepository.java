package com.suvidha.billing.repository;

import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

    Optional<Bill> findByAccount_IdAndStatus(String accountId, BillStatus status);

    List<Bill> findByAccount_IdOrderByDueDateDesc(String accountId);

    List<Bill> findByAccount_IdAndStatusInOrderByDueDateDesc(String accountId, List<BillStatus> statuses);

    Optional<Bill> findTopByAccount_IdOrderByDueDateDesc(String accountId);

    List<Bill> findByCitizenIdAndStatusIn(String citizenId, List<BillStatus> statuses);
}
