package com.fcv.citas.application.exception;

/** Una regla de negocio impide la operación; expone un código estable (HTTP 409). */
public class BusinessRuleException extends RuntimeException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
