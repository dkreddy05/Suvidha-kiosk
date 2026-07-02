package com.suvidha.billing.security;

/**
 * Stored in Authentication.getDetails() so services can access mobile.
 */
public class CitizenAuthDetails {
    private final String mobile;

    public CitizenAuthDetails(String mobile) {
        this.mobile = mobile;
    }

    public String getMobile() {
        return mobile;
    }
}
