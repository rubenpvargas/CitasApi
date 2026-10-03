package com.fcv.citas.application.exception;

/** Entrada semánticamente inválida detectada en aplicación/dominio (HTTP 400 VALIDATION_ERROR). */
public class RequestValidationException extends RuntimeException {
    private final String field;

    public RequestValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
