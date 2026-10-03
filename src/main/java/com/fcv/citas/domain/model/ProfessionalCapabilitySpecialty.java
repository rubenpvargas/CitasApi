package com.fcv.citas.domain.model;

/** Especialidad activa asignada a un profesional; a lo sumo una es primaria. */
public record ProfessionalCapabilitySpecialty(long id, String code, String name, boolean primary) {
}
