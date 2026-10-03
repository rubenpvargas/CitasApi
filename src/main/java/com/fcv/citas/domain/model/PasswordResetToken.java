package com.fcv.citas.domain.model;

import java.time.Instant;

/**
 * Token de recuperación de contraseña. Solo se conoce su hash; es temporal, de un solo uso y queda
 * invalidado cuando el usuario solicita uno nuevo.
 */
public record PasswordResetToken(
        Long id,
        long userId,
        String tokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt,
        Instant invalidatedAt
) {
    public boolean isUsableAt(Instant instant) {
        return usedAt == null && invalidatedAt == null && expiresAt.isAfter(instant);
    }

    public PasswordResetToken consume(Instant at) {
        if (!isUsableAt(at)) {
            throw new IllegalStateException("Password reset token is not usable");
        }
        return new PasswordResetToken(id, userId, tokenHash, createdAt, expiresAt, at, invalidatedAt);
    }

    public PasswordResetToken invalidate(Instant at) {
        return new PasswordResetToken(id, userId, tokenHash, createdAt, expiresAt, usedAt, at);
    }
}
