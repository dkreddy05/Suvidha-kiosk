package com.suvidha.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import com.suvidha.auth.Dto.Role;

@Getter
@Setter
@Entity
@Table(name = "users_auth", uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile")
})
public class UsersAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;
    @Column(nullable = false)
    private String mobile;

    private String aadhar;
    private String name;
    private String languagePreference;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Instant createdAt;

    public UsersAuth() {
    }

    public UsersAuth(String mobile, String aadhar,
            String name, String languagePreference,
            Role role, Instant createdAt) {
        this.mobile = mobile;
        this.aadhar = aadhar;
        this.name = name;
        this.languagePreference = languagePreference;
        this.role = role;
        this.createdAt = createdAt;
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
}