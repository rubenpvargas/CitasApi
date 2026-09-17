package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.TokenPair;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessExpiresAt,
        Instant refreshExpiresAt
) {
    static TokenResponse from(TokenPair pair) {
        return new TokenResponse(pair.accessToken(), pair.refreshToken(), "Bearer",
                pair.accessExpiresAt(), pair.refreshExpiresAt());
    }
}
