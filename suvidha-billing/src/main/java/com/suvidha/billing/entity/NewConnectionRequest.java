package com.suvidha.billing.entity;

import com.suvidha.billing.enums.ConnectionStatus;
import com.suvidha.billing.enums.ServiceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "new_connection_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewConnectionRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @Column(name = "citizen_id", nullable = false)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "address", nullable = false, length = 512)
    private String address;

    @Column(name = "property_type", nullable = false, length = 64)
    private String propertyType;

    @Column(name = "provider_name")
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConnectionStatus status;

    @Column(name = "ref_no", nullable = false, unique = true)
    private String refNo;
}
