package com.fcv.citas.application.port.out;

/** Genera tokens opacos impredecibles para recuperación de contraseña. */
public interface ResetTokenGeneratorPort {
    String newToken();
}
