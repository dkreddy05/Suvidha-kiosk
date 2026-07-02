package com.suvidha.notification.repository;

import com.suvidha.notification.entity.Notification;
import com.suvidha.notification.entity.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByCitizenIdOrderByCreatedAtDesc(String citizenId, Pageable pageable);

    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, Instant since);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.phoneNumber = :phoneNumber AND n.createdAt >= :since")
    long countRecentByPhone(@Param("phoneNumber") String phoneNumber, @Param("since") Instant since);
}
