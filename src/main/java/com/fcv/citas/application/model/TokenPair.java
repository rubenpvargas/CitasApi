package com.fcv.citas.application.model;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        String refreshToken,
        Instant accessExpiresAt,
        Instant refreshExpiresAt,
        String refreshJti
) {
}
