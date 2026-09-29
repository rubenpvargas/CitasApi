package com.fcv.citas.application.port.out;

import com.fcv.citas.application.model.RefreshTokenClaims;
import com.fcv.citas.application.model.TokenPair;
import com.fcv.citas.domain.model.User;

import java.time.Instant;

public interface TokenPort {
    TokenPair issuePair(User user, Instant issuedAt);
    RefreshTokenClaims parseRefresh(String token);
}
