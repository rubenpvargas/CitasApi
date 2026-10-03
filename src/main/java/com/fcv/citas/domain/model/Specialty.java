package com.fcv.citas.domain.model;

import java.util.Set;

/**
 * Especialidad del catálogo administrable. Su duración (30 o 60 min) es la referencia para nuevas
 * reservas; la general se aprueba automáticamente y las demás requieren aprobación ADMIN.
 */
public record Specialty(Long id, String code, String name, int durationMinutes, boolean general, boolean active) {
    public static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 60);

    public static boolean isAllowedDuration(int minutes) {
        return ALLOWED_DURATIONS.contains(minutes);
    }

    public boolean requiresAdminApproval() {
        return !general;
    }

    /** Número de slots consecutivos de 30 minutos que exige una cita de esta especialidad. */
    public int requiredSlots() {
        return durationMinutes / 30;
    }
}
