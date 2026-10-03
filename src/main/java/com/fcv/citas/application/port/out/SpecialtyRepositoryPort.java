package com.fcv.citas.application.port.out;

import com.fcv.citas.domain.model.Specialty;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepositoryPort {
    List<Specialty> findAll();

    Optional<Specialty> findById(long id);

    boolean codeExists(String code);

    boolean nameExists(String name, Long excludingId);

    boolean activeGeneralExists(Long excludingId);

    Specialty insert(String code, String name, int durationMinutes, boolean general);

    void update(long id, String name, int durationMinutes, boolean active);
}
