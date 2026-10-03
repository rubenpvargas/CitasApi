package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.PasswordResetRequestResult;

public interface PasswordRecoveryUseCase {
    /** Genera un token temporal si la cuenta existe y está activa; el resultado no revela su existencia. */
    PasswordResetRequestResult requestReset(String email);

    /** Consume atómicamente un token vigente y fija la nueva contraseña; si no es válido lanza INVALID_RESET_TOKEN. */
    void confirmReset(String token, String newPassword);
}
