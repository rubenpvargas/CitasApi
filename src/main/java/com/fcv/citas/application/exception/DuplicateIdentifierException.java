package com.fcv.citas.application.exception;

public class DuplicateIdentifierException extends RuntimeException {
    public DuplicateIdentifierException() {
        super("An account with the supplied identifier already exists");
    }
}
