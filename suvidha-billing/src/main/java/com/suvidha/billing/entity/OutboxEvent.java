package com.suvidha.billing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column
    private int retryCount;

    @Column
    private String errorMessage;

    @Column
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum OutboxStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = OutboxStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.retryCount++;
        this.errorMessage = error;
        if (this.retryCount >= 5) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.status = OutboxStatus.PENDING;
        }
    }
}
