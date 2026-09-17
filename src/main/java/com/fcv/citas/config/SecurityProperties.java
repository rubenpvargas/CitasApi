package com.fcv.citas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String issuer,
        String accessSecret,
        String refreshSecret,
        long accessMinutes,
        long refreshDays
) {
    public SecurityProperties {
        if (accessSecret == null || accessSecret.length() < 32
                || refreshSecret == null || refreshSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secrets must contain at least 32 characters");
        }
        if (accessSecret.equals(refreshSecret)) {
            throw new IllegalArgumentException("Access and refresh JWT secrets must be different");
        }
        if (accessMinutes <= 0 || refreshDays <= 0) {
            throw new IllegalArgumentException("JWT expiration values must be positive");
        }
    }
}
