package com.suvidha.grievance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "grievance_updates", schema = "grievance")
public class GrievanceUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grievance_id", nullable = false)
    private Grievance grievance;

    @Column(nullable = false)
    private String status;

    @Column(length = 500)
    private String message;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GrievanceUpdate() {}

    public GrievanceUpdate(String status, String message, Instant updatedAt) {
        this.status = status;
        this.message = message;
        this.updatedAt = updatedAt;
    }
}
