package com.suvidha.billing.entity;

import com.suvidha.billing.enums.LinkRequestStatus;
import com.suvidha.billing.enums.UtilityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_link_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String citizenId;

    @Column(nullable = false)
    private String accountNo;

    @Column(nullable = false)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UtilityType utilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkRequestStatus status;

    private int attemptCount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getRefNo() {
        return id.toString();
    }
}
