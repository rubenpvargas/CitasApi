package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.AvailabilityOffer;
import com.fcv.citas.application.model.AvailabilityQuery;
import com.fcv.citas.application.model.CandidateSlot;
import com.fcv.citas.application.port.out.AvailabilityQueryPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-015 — consulta de disponibilidad: filtros, duración de la especialidad y exclusiones. */
class AvailabilityQueryServiceTest {
    private final SpecialtyServiceTest.InMemorySpecialties specialties = new SpecialtyServiceTest.InMemorySpecialties();
    private final FakeQuery port = new FakeQuery();
    private final AvailabilityQueryService service = new AvailabilityQueryService(port, specialties, CLOCK);

    private static CandidateSlot slot(long block, long professional, String time) {
        return new CandidateSlot(block, LocalDateTime.of(TODAY.plusDays(1), LocalTime.parse(time)), professional,
                "Valeria Sintetica", "HIC", "Sede HIC");
    }

    @Test
    void ca02SixtyMinuteSpecialtyOffersOnlyStartsWithTwoConsecutiveFreeSlots() {
        long ortho = specialties.insert("ORTO", "Ortopedia", 60, false).id();
        port.slots.addAll(List.of(slot(1, 7, "08:00"), slot(1, 7, "08:30"), slot(1, 7, "09:30")));

        List<AvailabilityOffer> offers = service.search(new AvailabilityQuery(ortho, TODAY, TODAY.plusDays(2), null, null));

        assertThat(offers).hasSize(1);
        AvailabilityOffer offer = offers.getFirst();
        assertThat(offer.startAt()).isEqualTo(TODAY.plusDays(1).atTime(8, 0));
        assertThat(offer.endAt()).isEqualTo(TODAY.plusDays(1).atTime(9, 0));
        assertThat(offer.durationMinutes()).isEqualTo(60);
        assertThat(offer.professionalId()).isEqualTo(7L);
        assertThat(offer.specialtyName()).isEqualTo("Ortopedia");
        assertThat(port.lastSpecialtyId).isEqualTo(ortho);
    }

    @Test
    void ca01FiltersArePassedToTheQuery() {
        long general = specialties.insert("GEN", "General", 30, true).id();

        service.search(new AvailabilityQuery(general, TODAY, TODAY, "icv", 7L));

        assertThat(port.lastLocationCode).isEqualTo("ICV");
        assertThat(port.lastProfessionalId).isEqualTo(7L);
    }

    @Test
    void ca03InactiveSpecialtyOffersNothingAndUnknownIsNotFound() {
        long inactive = specialties.insert("OFF", "Off", 30, false).id();
        specialties.update(inactive, "Off", 30, false);
        port.slots.add(slot(1, 7, "08:00"));

        assertThat(service.search(new AvailabilityQuery(inactive, TODAY, TODAY.plusDays(1), null, null))).isEmpty();
        assertThatThrownBy(() -> service.search(new AvailabilityQuery(999L, TODAY, TODAY, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rangeIsBounded() {
        long general = specialties.insert("GEN", "General", 30, true).id();

        assertThatThrownBy(() -> service.search(new AvailabilityQuery(general, TODAY, TODAY.plusDays(31), null, null)))
                .isInstanceOf(RequestValidationException.class);
    }

    private static final class FakeQuery implements AvailabilityQueryPort {
        final List<CandidateSlot> slots = new ArrayList<>();
        Long lastSpecialtyId;
        String lastLocationCode;
        Long lastProfessionalId;

        @Override
        public List<CandidateSlot> findFreeSlots(long specialtyId, java.time.LocalDate from, java.time.LocalDate to,
                                                 String locationCode, Long professionalId) {
            lastSpecialtyId = specialtyId;
            lastLocationCode = locationCode;
            lastProfessionalId = professionalId;
            return slots;
        }
    }
}
