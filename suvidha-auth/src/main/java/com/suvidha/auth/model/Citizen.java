package com.suvidha.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import com.suvidha.auth.Dto.Role;

@Getter
@Setter
@Entity
@Table(name = "citizens_table", uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile"),
        @UniqueConstraint(columnNames = "aadhar")
})
public class Citizen {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;
    @Column(nullable = false)
    private String mobile;
    @Column(name = "aadhar_hash", unique = true, nullable = false, length = 64)
    private String aadharHash;

    @Convert(converter = AadharEncryptionConverter.class)
    @Column(name = "aadhar", nullable = false)
    private String aadhar;

    private String name;
    private String languagePreference;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "consumer_id", unique = true, nullable = false, length = 50)
    private String consumerId;

    private Instant createdAt;

    public Citizen() {
    }

    public Citizen(String mobile, String aadhar,
            String name, String languagePreference,
            Role role, Instant createdAt) {
        this.mobile = mobile;
        this.aadhar = aadhar;
        this.name = name;
        this.languagePreference = languagePreference;
        this.role = role;
        this.createdAt = createdAt;
    }

    @PrePersist
    @PreUpdate
    private void computeAadharHash() {
        if (this.aadhar != null) {
            this.aadharHash = AadharEncryptionConverter.generateAadharHash(this.aadhar);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAadhar() {
        return aadhar;
    }

    public void setAadhar(String aadhar) {
        this.aadhar = aadhar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguagePreference() {
        return languagePreference;
    }

    public void setLanguagePreference(String languagePreference) {
        this.languagePreference = languagePreference;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}
