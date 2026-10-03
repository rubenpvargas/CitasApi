package com.fcv.citas.domain.model;

/** Vínculo usuario → profesional usado para derivar el actor de agenda desde el JWT. */
public record ProfessionalAccount(long professionalId, long userId, boolean active) {
}
