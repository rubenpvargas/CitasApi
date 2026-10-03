package com.fcv.citas.application.exception;

/** El actor autenticado no tiene permiso por rol u ownership sobre la operación (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
