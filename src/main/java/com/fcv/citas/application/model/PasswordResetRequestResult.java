package com.fcv.citas.application.model;

import java.util.Optional;

/**
 * Resultado de solicitar recuperación. Es idéntico exista o no la cuenta; solo con la bandera de
 * desarrollo activa transporta el token en claro para completar el flujo sin SMTP.
 */
public record PasswordResetRequestResult(Optional<String> developmentToken) {
    private static final PasswordResetRequestResult NONE = new PasswordResetRequestResult(Optional.empty());

    public static PasswordResetRequestResult none() {
        return NONE;
    }

    public static PasswordResetRequestResult withDevelopmentToken(String token) {
        return new PasswordResetRequestResult(Optional.of(token));
    }

    @Override
    public String toString() {
        return "PasswordResetRequestResult[developmentToken=" + (developmentToken.isPresent() ? "***" : "none") + "]";
    }
}
