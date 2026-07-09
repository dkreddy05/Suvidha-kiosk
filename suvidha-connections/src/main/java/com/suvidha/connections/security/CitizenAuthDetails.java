package com.suvidha.connections.security;

public class CitizenAuthDetails {
    private final String mobile;

    public CitizenAuthDetails(String mobile) {
        this.mobile = mobile;
    }

    public String getMobile() {
        return mobile;
    }
}
