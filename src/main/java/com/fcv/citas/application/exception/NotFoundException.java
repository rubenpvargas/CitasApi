package com.fcv.citas.application.exception;

/** El recurso solicitado no existe o no es visible para quien lo solicita (HTTP 404). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
