package com.fcv.citas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param exposeDevelopmentToken solo en laboratorio local: devuelve el token en la respuesta (por defecto false)
 * @param tokenMinutes           vigencia del token de recuperación
 */
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(boolean exposeDevelopmentToken, long tokenMinutes) {
    public PasswordResetProperties {
        if (tokenMinutes <= 0) {
            throw new IllegalArgumentException("Password reset token minutes must be positive");
        }
    }
}
