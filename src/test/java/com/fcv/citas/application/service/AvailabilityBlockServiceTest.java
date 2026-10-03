package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.ForbiddenException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.BlockCommand;
import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static com.fcv.citas.application.service.ProfessionalAdminServiceTest.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-012 — publicación de bloques: actor del JWT, estado, sede asignada, tiempo y solape. */
class AvailabilityBlockServiceTest {
    static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    static final Clock CLOCK = Clock.fixed(LocalDateTime.of(TODAY, LocalTime.of(10, 0)).atZone(BOGOTA).toInstant(), BOGOTA);
    static final long USER_ID = 50L;

    final InMemoryProfessionals professionals = new InMemoryProfessionals();
    final InMemoryBlocks blocks = new InMemoryBlocks(professionals);
    final AvailabilityBlockService service = new AvailabilityBlockService(blocks, professionals, new DirectTransactions(), CLOCK);
    long professionalId;

    @BeforeEach
    void seed() {
        professionals.locations.put(1L, new Location(1L, "HIC", "Sede HIC", "a", "c", "d", true));
        professionals.locations.put(2L, new Location(2L, "ICV", "Sede ICV", "a", "c", "d", true));
        professionalId = 10L;
        professionals.professionals.put(professionalId, new ProfessionalSummary(professionalId, USER_ID, "Valeria", "Sintetica",
                "v@example.test", "300", "P1", "L1", true, List.of(),
                List.of(professionals.locations.get(1L), professionals.locations.get(2L))));
    }

    static BlockCommand cmd(LocalDate date, String start, String end, String location) {
        return new BlockCommand(date, LocalTime.parse(start), LocalTime.parse(end), location);
    }

    @Test
    void ca01PublishesFutureBlockDiscretizedInThirtyMinuteSlots() {
        AvailabilityBlock block = service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "10:00", "HIC"));

        assertThat(block.totalSlots()).isEqualTo(4);
        assertThat(block.committedSlots()).isZero();
        assertThat(block.locationCode()).isEqualTo("HIC");
        assertThat(blocks.rows.get(block.id()).slots).hasSize(4);
    }

    @Test
    void sameDayLaterTodayIsAllowedButStartAtOrBeforeNowIsPast() {
        assertThat(service.create(USER_ID, cmd(TODAY, "10:30", "11:00", "HIC")).totalSlots()).isOne();
        assertCode(() -> service.create(USER_ID, cmd(TODAY, "10:00", "11:00", "ICV")), "PAST_BLOCK");
    }

    @Test
    void ca02OverlapInAnotherLocationIsRejectedWithoutTouchingPreviousAgenda() {
        service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "10:00", "HIC"));

        assertCode(() -> service.create(USER_ID, cmd(TODAY.plusDays(1), "09:00", "11:00", "ICV")), "BLOCK_OVERLAP");
        assertThat(blocks.rows).hasSize(1);
    }

    @Test
    void misalignedOrInvertedTimesAre400() {
        assertThatThrownBy(() -> service.create(USER_ID, cmd(TODAY.plusDays(1), "08:10", "09:00", "HIC")))
                .isInstanceOf(RequestValidationException.class).extracting("field").isEqualTo("startTime");
        assertThatThrownBy(() -> service.create(USER_ID, cmd(TODAY.plusDays(1), "09:00", "08:00", "HIC")))
                .isInstanceOf(RequestValidationException.class).extracting("field").isEqualTo("endTime");
    }

    @Test
    void locationMustBeAssignedAndKnown() {
        professionals.locations.put(3L, new Location(3L, "OTRA", "Otra", "a", "c", "d", true));

        assertCode(() -> service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "OTRA")), "LOCATION_NOT_ASSIGNED");
        assertThatThrownBy(() -> service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "NOPE")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void inactiveProfessionalCannotPublishAndUserWithoutProfileIsForbidden() {
        ProfessionalSummary p = professionals.professionals.get(professionalId);
        professionals.professionals.put(professionalId, new ProfessionalSummary(p.id(), p.userId(), p.firstName(),
                p.lastName(), p.email(), p.phone(), p.professionalCode(), p.licenseNumber(), false, p.specialties(), p.locations()));

        assertCode(() -> service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC")), "PROFESSIONAL_INACTIVE");
        assertThatThrownBy(() -> service.create(999L, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC")))
                .isInstanceOf(ForbiddenException.class);
    }
}
