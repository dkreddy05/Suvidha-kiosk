package com.suvidha.billing.entity;

import com.suvidha.billing.enums.ServiceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_account_service_type_account_no", columnNames = {
                "service_type",
                "account_no"
        })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceAccount {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "citizen_id", nullable = false)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "account_no", nullable = false)
    private String accountNo;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "registered_mobile", nullable = false)
    private String registeredMobile;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "account")
    @Builder.Default
    private List<Bill> bills = new ArrayList<>();
}
