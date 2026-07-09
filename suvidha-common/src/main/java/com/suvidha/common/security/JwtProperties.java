package com.suvidha.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "suvidha.jwt")
public class JwtProperties {

    private String publicKeyUrl = "http://localhost:8081/api/auth/public-key";
    private String issuer = "suvidha-auth";
    private String audience = "suvidha-services";

    public String getPublicKeyUrl() {
        return publicKeyUrl;
    }

    public void setPublicKeyUrl(String publicKeyUrl) {
        this.publicKeyUrl = publicKeyUrl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
