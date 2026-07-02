package com.suvidha.billing.repository;

import com.suvidha.billing.entity.KioskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KioskLogRepository extends JpaRepository<KioskLog, UUID> {
}
