package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.domain.model.Specialty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecialtyServiceTest {
    private final InMemorySpecialties repo = new InMemorySpecialties();
    private final SpecialtyService service = new SpecialtyService(repo, new DirectTransactions());

    @ParameterizedTest
    @ValueSource(ints = {0, 15, 45, 90})
    void onlyThirtyOrSixtyMinutesAreAccepted(int minutes) {
        assertThatThrownBy(() -> service.create("X", "X", minutes, false))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field").isEqualTo("durationMinutes");
        Specialty ok = service.create("OK", "Ok", 60, false);
        assertThatThrownBy(() -> service.update(ok.id(), "Ok", minutes, true))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void createsSpecializedRequiringApprovalAndNormalizesCode() {
        Specialty s = service.create(" cardio ", " Cardiologia ", 30, false);

        assertThat(s.code()).isEqualTo("CARDIO");
        assertThat(s.name()).isEqualTo("Cardiologia");
        assertThat(s.requiresAdminApproval()).isTrue();
        assertThat(s.active()).isTrue();
    }

    @Test
    void atMostOneActiveGeneralSpecialty() {
        Specialty general = service.create("GENERAL", "General", 30, true);
        assertThat(general.requiresAdminApproval()).isFalse();

        assertThatThrownBy(() -> service.create("GENERAL_2", "General 2", 30, true))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("GENERAL_SPECIALTY_CONFLICT");

        service.update(general.id(), "General", 30, false);
        Specialty second = service.create("GENERAL_2", "General 2", 30, true);
        assertThatThrownBy(() -> service.update(general.id(), "General", 30, true))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("GENERAL_SPECIALTY_CONFLICT");
        assertThat(service.update(second.id(), "General dos", 60, true).durationMinutes()).isEqualTo(60);
    }

    @Test
    void duplicatesAndUnknownIds() {
        service.create("DERMA", "Dermatologia", 30, false);

        assertThatThrownBy(() -> service.create("derma", "Otra", 30, false))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("DUPLICATE_CODE");
        assertThatThrownBy(() -> service.create("DERMA2", "Dermatologia", 30, false))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("DUPLICATE_NAME");
        assertThatThrownBy(() -> service.update(999L, "x", 30, true)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivationIsLogicalAndPublicListingShowsOnlyActive() {
        Specialty a = service.create("A", "A", 30, false);
        Specialty b = service.create("B", "B", 60, false);
        service.update(b.id(), "B", 60, false);

        assertThat(service.listAll()).extracting(Specialty::id).containsExactly(a.id(), b.id());
        assertThat(service.listActive()).extracting(Specialty::id).containsExactly(a.id());
    }

    static final class InMemorySpecialties implements SpecialtyRepositoryPort {
        private final Map<Long, Specialty> byId = new LinkedHashMap<>();
        private long next = 1;

        @Override public List<Specialty> findAll() { return new ArrayList<>(byId.values()); }
        @Override public Optional<Specialty> findById(long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public boolean codeExists(String code) { return byId.values().stream().anyMatch(s -> s.code().equals(code)); }
        @Override public boolean nameExists(String name, Long excludingId) {
            return byId.values().stream().anyMatch(s -> s.name().equalsIgnoreCase(name) && !s.id().equals(excludingId));
        }
        @Override public boolean activeGeneralExists(Long excludingId) {
            return byId.values().stream().anyMatch(s -> s.general() && s.active() && !s.id().equals(excludingId));
        }
        @Override public Specialty insert(String code, String name, int durationMinutes, boolean general) {
            Specialty s = new Specialty(next++, code, name, durationMinutes, general, true);
            byId.put(s.id(), s);
            return s;
        }
        @Override public void update(long id, String name, int durationMinutes, boolean active) {
            Specialty s = byId.get(id);
            byId.put(id, new Specialty(id, s.code(), name, durationMinutes, s.general(), active));
        }
    }
}
