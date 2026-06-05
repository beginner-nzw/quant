package com.quant.common.security;

public class AuthProperties {

    private String mode = AuthMode.JWT.name();
    private String jwtSecret;
    private String issuer;
    private String audience;
    private String userIdClaim = "sub";
    private String roleClaim = "role";
    private long clockSkewSeconds = 60L;

    public AuthMode authMode() {
        return AuthMode.from(mode);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
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

    public String getUserIdClaim() {
        return userIdClaim;
    }

    public void setUserIdClaim(String userIdClaim) {
        this.userIdClaim = userIdClaim;
    }

    public String getRoleClaim() {
        return roleClaim;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }
}
