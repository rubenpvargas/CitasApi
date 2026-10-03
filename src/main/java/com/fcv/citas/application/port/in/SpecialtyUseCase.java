package com.fcv.citas.application.port.in;

import com.fcv.citas.domain.model.Specialty;

import java.util.List;

/** HU-009 — catálogo de especialidades (gestión ADMIN y lectura de activas). */
public interface SpecialtyUseCase {
    List<Specialty> listAll();

    List<Specialty> listActive();

    Specialty create(String code, String name, int durationMinutes, boolean general);

    Specialty update(long id, String name, int durationMinutes, boolean active);
}
