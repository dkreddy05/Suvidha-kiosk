package com.suvidha.auth.Dto;

public class UserAuthDto {
    private String Id;
    private String mobile;
    private String aadhar;
    private String name;
    private String languagePreference;
    private String role;

    public UserAuthDto(String Id, String mobile, String aadhar, String name, String languagePreference,
            String role) {
        this.Id = Id;
        this.mobile = mobile;
        this.aadhar = aadhar;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}
