package com.fcv.citas.domain.model;

/** Violación de una regla de dominio con código estable (se expone como HTTP 409). */
public class DomainRuleViolation extends RuntimeException {
    private final String code;

    public DomainRuleViolation(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
