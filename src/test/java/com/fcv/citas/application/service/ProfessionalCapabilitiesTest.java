package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.model.CapabilitiesCommand;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.fcv.citas.application.service.ProfessionalAdminServiceTest.assertCode;
import static com.fcv.citas.application.service.ProfessionalAdminServiceTest.command;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-011 — capacidades: primaria entre las asignadas, catálogos activos, estado operativo. */
class ProfessionalCapabilitiesTest {
    private final InMemoryProfessionals repo = new InMemoryProfessionals();
    private final SpecialtyServiceTest.InMemorySpecialties specialties = new SpecialtyServiceTest.InMemorySpecialties();
    private final ProfessionalAdminService service = new ProfessionalAdminService(repo, specialties,
            new com.fcv.citas.application.port.out.PasswordHashPort() {
                @Override public String hash(String raw) { return "h"; }
                @Override public boolean matches(String raw, String encoded) { return false; }
            }, new DirectTransactions(), ProfessionalAdminServiceTest.CLOCK);
    private long professionalId;
    private long general;
    private long cardio;
    private long inactiveSpecialty;

    @BeforeEach
    void seed() {
        general = specialties.insert("GEN", "General", 30, true).id();
        cardio = specialties.insert("CAR", "Cardio", 30, false).id();
        inactiveSpecialty = specialties.insert("OFF", "Off", 30, false).id();
        specialties.update(inactiveSpecialty, "Off", 30, false);
        specialties.findAll().forEach(s -> repo.specialties.put(s.id(), s));
        repo.locations.put(1L, new Location(1L, "HIC", "HIC", "a", "c", "d", true));
        repo.locations.put(2L, new Location(2L, "ICV", "ICV", "a", "c", "d", true));
        repo.locations.put(3L, new Location(3L, "OLD", "Old", "a", "c", "d", false));
        professionalId = service.create(command("cap@example.test", "D1", "P1", "L1")).id();
    }

    @Test
    void ca01AssignsSpecialtiesLocationsAndPrimaryDeduplicating() {
        ProfessionalSummary p = service.configure(professionalId,
                new CapabilitiesCommand(List.of(general, cardio, cardio), cardio, List.of(1L, 2L, 1L), true));

        assertThat(p.specialties()).extracting(s -> s.id()).containsExactlyInAnyOrder(general, cardio);
        assertThat(p.primarySpecialtyId()).isEqualTo(cardio);
        assertThat(p.locations()).extracting(Location::code).containsExactly("HIC", "ICV");
        assertThat(p.active()).isTrue();
    }

    @Test
    void ca02PrimaryOutsideAssignedSpecialtiesIsRejected() {
        assertCode(() -> service.configure(professionalId,
                new CapabilitiesCommand(List.of(general), cardio, List.of(1L), true)), "PRIMARY_NOT_ASSIGNED");
    }

    @Test
    void inactiveSpecialtyOrLocationIsRejectedAndUnknownIsNotFound() {
        assertCode(() -> service.configure(professionalId,
                new CapabilitiesCommand(List.of(inactiveSpecialty), inactiveSpecialty, List.of(1L), true)), "CATALOG_INACTIVE");
        assertCode(() -> service.configure(professionalId,
                new CapabilitiesCommand(List.of(general), general, List.of(3L), true)), "CATALOG_INACTIVE");
        assertThatThrownBy(() -> service.configure(professionalId,
                new CapabilitiesCommand(List.of(999L), 999L, List.of(1L), true))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.configure(professionalId,
                new CapabilitiesCommand(List.of(general), general, List.of(99L), true))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.configure(424242L,
                new CapabilitiesCommand(List.of(general), general, List.of(1L), true))).isInstanceOf(NotFoundException.class);
        assertThat(repo.professionals.get(professionalId).specialties()).isEmpty();
    }

    @Test
    void ca03ProfessionalCanBeDeactivated() {
        ProfessionalSummary p = service.configure(professionalId,
                new CapabilitiesCommand(List.of(general), general, List.of(1L), false));

        assertThat(p.active()).isFalse();
    }
}
