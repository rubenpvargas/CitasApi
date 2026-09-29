package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.AuthenticatedSession;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessExpiresAt,
        Instant refreshExpiresAt,
        AuthenticatedUserResponse user
) {
    static LoginResponse from(AuthenticatedSession session) {
        var tokens = session.tokens();
        return new LoginResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer",
                tokens.accessExpiresAt(), tokens.refreshExpiresAt(), AuthenticatedUserResponse.from(session.user()));
    }
}
