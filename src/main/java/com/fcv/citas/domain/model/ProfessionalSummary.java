package com.fcv.citas.domain.model;

import java.util.List;

/** Profesional sintético con sus capacidades vigentes (especialidades y sedes activas). Sin credenciales. */
public record ProfessionalSummary(long id, long userId, String firstName, String lastName, String email, String phone,
                                  String professionalCode, String licenseNumber, boolean active,
                                  List<ProfessionalCapabilitySpecialty> specialties, List<Location> locations) {
    public ProfessionalSummary {
        specialties = List.copyOf(specialties);
        locations = List.copyOf(locations);
    }

    public Long primarySpecialtyId() {
        return specialties.stream().filter(ProfessionalCapabilitySpecialty::primary)
                .map(ProfessionalCapabilitySpecialty::id).findFirst().orElse(null);
    }
}
