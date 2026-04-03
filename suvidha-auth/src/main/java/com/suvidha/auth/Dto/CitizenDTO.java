package com.suvidha.auth.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CitizenDTO {
    private String id;
    private String mobile;
    private String aadhaarLast4;
    private String name;
    private String languagePref;
    private Instant createdAt;

    public CitizenDTO() {
    }

    public CitizenDTO(String id, String mobile, String aadhaarLast4, String name, String languagePref, Instant createdAt) {
        this.id = id;
        this.mobile = mobile;
        this.aadhaarLast4 = aadhaarLast4;
        this.name = name;
        this.languagePref = languagePref;
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

    public String getAadhaarLast4() {
        return aadhaarLast4;
    }

    public void setAadhaarLast4(String aadhaarLast4) {
        this.aadhaarLast4 = aadhaarLast4;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguagePref() {
        return languagePref;
    }

    public void setLanguagePref(String languagePref) {
        this.languagePref = languagePref;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
