package com.fcv.citas.application.port.out;

import java.time.Instant;

public interface RefreshSessionRevocationPort {
    /** Revoca todas las sesiones de refresh activas del usuario. */
    void revokeAllForUser(long userId, Instant at);
}
