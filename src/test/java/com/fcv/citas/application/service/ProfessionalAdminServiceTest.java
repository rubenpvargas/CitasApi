package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.InvalidPasswordException;
import com.fcv.citas.application.model.NewProfessionalCommand;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.domain.model.ProfessionalSummary;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-010 — alta de profesional por ADMIN. */
class ProfessionalAdminServiceTest {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T15:00:00Z"), ZoneId.of("America/Bogota"));
    final InMemoryProfessionals repo = new InMemoryProfessionals();
    final SpecialtyServiceTest.InMemorySpecialties specialties = new SpecialtyServiceTest.InMemorySpecialties();
    final ProfessionalAdminService service = new ProfessionalAdminService(repo, specialties, new PasswordHashPort() {
        @Override public String hash(String raw) { return "bcrypt(" + raw + ")"; }
        @Override public boolean matches(String raw, String encoded) { return false; }
    }, new DirectTransactions(), CLOCK);

    static NewProfessionalCommand command(String email, String document, String code, String license) {
        return new NewProfessionalCommand("Valeria", "Sintetica", "cc", document, email, "3000000000",
                "ClaveProf2026", code, license);
    }

    @Test
    void ca01CreatesActiveProfessionalWithNormalizedIdentifiersAndHashedPassword() {
        ProfessionalSummary created = service.create(command("  Valeria.Prof@Example.TEST ", " d-1 ", " prof-1 ", " RM-1 "));

        assertThat(created.email()).isEqualTo("valeria.prof@example.test");
        assertThat(created.professionalCode()).isEqualTo("PROF-1");
        assertThat(created.licenseNumber()).isEqualTo("RM-1");
        assertThat(created.active()).isTrue();
        assertThat(repo.documents.get(created.id())).isEqualTo("CC:D-1");
        assertThat(repo.passwordHashes.get(created.id())).isEqualTo("bcrypt(ClaveProf2026)");
    }

    @Test
    void duplicatesHaveSpecificCodes() {
        service.create(command("a@example.test", "D1", "P1", "L1"));

        assertCode(() -> service.create(command("A@EXAMPLE.TEST", "D2", "P2", "L2")), "DUPLICATE_EMAIL");
        assertCode(() -> service.create(command("b@example.test", "d1", "P2", "L2")), "DUPLICATE_DOCUMENT");
        assertCode(() -> service.create(command("b@example.test", "D2", "p1", "L2")), "DUPLICATE_PROFESSIONAL_CODE");
        assertCode(() -> service.create(command("b@example.test", "D2", "P2", "L1")), "DUPLICATE_LICENSE");
        assertThat(repo.professionals).hasSize(1);
    }

    @Test
    void passwordMustSatisfyTheSharedPolicy() {
        NewProfessionalCommand weak = new NewProfessionalCommand("V", "S", "CC", "D9", "w@example.test", "300",
                "debil", "P9", "L9");

        assertThatThrownBy(() -> service.create(weak)).isInstanceOf(InvalidPasswordException.class);
        assertThat(repo.professionals).isEmpty();
    }

    static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo(code);
    }
}
