package com.suvidha.connections.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "connection_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ConnectionRequest {

    @Id
    private UUID id;

    @Column(name = "display_id", nullable = false, unique = true)
    private String displayId;

    @Column(name = "citizen_id", nullable = false)
    private String citizenId;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "address", nullable = false, length = 512)
    private String address;

    @Column(name = "status", nullable = false)
    private String status;

    @CreatedDate
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "estimated_days", nullable = false)
    private int estimatedDays;

    @Version
    private Long version;

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConnectionTimeline> timeline = new ArrayList<>();

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConnectionDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConnectionStatusHistory> statusHistory = new ArrayList<>();
}
