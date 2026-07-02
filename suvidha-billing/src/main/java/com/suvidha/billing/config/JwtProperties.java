package com.suvidha.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String citizenIdClaim = "citizenId";
    private String mobileClaim = "mobile";
    private String roleClaim = "role";

    public String getCitizenIdClaim() { return citizenIdClaim; }
    public void setCitizenIdClaim(String citizenIdClaim) { this.citizenIdClaim = citizenIdClaim; }
    public String getMobileClaim() { return mobileClaim; }
    public void setMobileClaim(String mobileClaim) { this.mobileClaim = mobileClaim; }
    public String getRoleClaim() { return roleClaim; }
    public void setRoleClaim(String roleClaim) { this.roleClaim = roleClaim; }
}
