package com.fcv.citas.application.exception;

import com.fcv.citas.domain.model.PasswordPolicy;

/** La contraseña propuesta no cumple la {@link PasswordPolicy} (HTTP 400 VALIDATION_ERROR). */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("Password " + PasswordPolicy.DESCRIPTION);
    }
}
