package com.fcv.citas.application.port.out;

import java.time.Instant;

public interface UserCredentialPort {
    void updatePasswordHash(long userId, String passwordHash, Instant at);
}
