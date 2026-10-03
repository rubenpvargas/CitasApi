package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.CapabilitiesCommand;
import com.fcv.citas.application.model.NewProfessionalCommand;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalSummary;

import java.util.List;

/** HU-010/HU-011 — alta y capacidades de profesionales (ADMIN). */
public interface ProfessionalAdminUseCase {
    ProfessionalSummary create(NewProfessionalCommand command);

    List<ProfessionalSummary> list();

    ProfessionalSummary configure(long professionalId, CapabilitiesCommand command);

    List<Location> locations();
}
