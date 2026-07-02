package com.suvidha.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kiosk_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "kiosk_id", nullable = false)
    private String kioskId;

    @Column(name = "citizen_id")
    private String citizenId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "path")
    private String path;

    @Column(name = "ref_no")
    private String refNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
