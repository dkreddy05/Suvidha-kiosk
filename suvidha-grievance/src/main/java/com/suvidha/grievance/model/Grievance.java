package com.suvidha.grievance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "grievances", schema = "grievance")
public class Grievance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "citizen_id", nullable = false)
    private String citizenId;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @Column(nullable = false)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "account_id")
    private String accountId;

    @Column(nullable = false)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GrievanceUpdate> updates = new ArrayList<>();

    public void addUpdate(GrievanceUpdate update) {
        updates.add(update);
        update.setGrievance(this);
    }
}
