package com.suvidha.auth.Dto;

public class UserAuthDto {
    private String Id;
    private String mobile;
    private String name;
    private String languagePreference;
    private String role;
    private String consumerId;

    public UserAuthDto(String Id, String mobile, String name, String languagePreference, String role) {
        this.Id = Id;
        this.mobile = mobile;
        this.name = name;
        this.languagePreference = languagePreference;
        this.role = role;
    }

    public UserAuthDto() {
    }

    public String getId() {
        return Id;
    }

    public void setId(String Id) {
        this.Id = Id;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}
