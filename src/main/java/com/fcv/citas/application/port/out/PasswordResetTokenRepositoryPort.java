package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {
    /** Invalida los tokens no usados ni invalidados del usuario. */
    void invalidateActiveForUser(long userId, Instant at);

    PasswordResetToken save(PasswordResetToken token);

    /** Busca por hash bloqueando la fila hasta el fin de la transacción (consumo atómico). */
    Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash);
}
