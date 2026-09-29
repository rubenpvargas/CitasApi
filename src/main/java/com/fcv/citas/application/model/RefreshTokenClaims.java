package com.fcv.citas.application.model;

import java.time.Instant;

public record RefreshTokenClaims(Long userId, String jti, Instant expiresAt) {
}
