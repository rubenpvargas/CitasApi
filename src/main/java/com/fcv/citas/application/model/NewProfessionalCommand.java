package com.fcv.citas.application.model;

/** Datos sintéticos para que ADMIN cree un usuario PROFESSIONAL. */
public record NewProfessionalCommand(String firstName, String lastName, String documentType, String documentNumber,
                                     String email, String phone, String password, String professionalCode,
                                     String licenseNumber) {
    @Override
    public String toString() {
        return "NewProfessionalCommand[email=" + email + ", professionalCode=" + professionalCode + ", password=***]";
    }
}
