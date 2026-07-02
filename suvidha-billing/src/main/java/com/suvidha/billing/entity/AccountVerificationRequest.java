package com.suvidha.billing.entity;

import com.suvidha.billing.enums.VerificationStatus;
import com.suvidha.billing.enums.ServiceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_verification_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountVerificationRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @Column(name = "citizen_id", nullable = false, length = 128)
    private String citizenId;

    @Column(name = "consumer_no", nullable = false, length = 128)
    private String consumerNo;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    private String accountHolderName;

    @Column(name = "registered_mobile", nullable = false, length = 32)
    private String registeredMobile;

    @Column(name = "address", nullable = false, length = 512)
    private String address;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VerificationStatus status;

    @Column(name = "ref_no", nullable = false, unique = true, length = 128)
    private String refNo;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
