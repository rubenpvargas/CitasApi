package com.fcv.citas.domain.model;

import java.time.Instant;

public record RefreshSession(
        Long id,
        Long userId,
        String tokenHash,
        String jti,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        String replacedByJti
) {
    public boolean isActiveAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant);
    }

    public RefreshSession revoke(Instant revokedAt, String replacementJti) {
        return new RefreshSession(id, userId, tokenHash, jti, issuedAt, expiresAt,
                revokedAt, replacementJti);
    }
}
