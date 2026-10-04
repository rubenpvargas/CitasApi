package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.ForbiddenException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;
import com.fcv.citas.domain.model.ProfessionalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-023 — agenda propia: actor del JWT, rango acotado, filtro de sede y elegibilidad de cierre. */
class ProfessionalAgendaServiceTest {
    final InMemoryProfessionals professionals = new InMemoryProfessionals();
    final AgendaAppointments appointments = new AgendaAppointments();
    final ProfessionalAgendaService service = new ProfessionalAgendaService(appointments, professionals,
            new DirectTransactions(), CLOCK);

    @BeforeEach
    void seed() {
        professionals.professionals.put(10L, new ProfessionalSummary(10L, 50L, "V", "S", "v@x.test", "1", "P1", "L1",
                true, List.of(), List.of()));
    }

    @Test
    void ca01AgendaUsesTheProfessionalFromTheTokenAndNormalizesLocation() {
        service.agenda(50L, TODAY, TODAY.plusDays(6), " hic ");

        assertThat(appointments.lastProfessionalId).isEqualTo(10L);
        assertThat(appointments.lastLocation).isEqualTo("HIC");
    }

    @Test
    void ca02UserWithoutProfessionalProfileIsForbiddenAndRangeIsBounded() {
        assertThatThrownBy(() -> service.agenda(51L, TODAY, TODAY, null)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.agenda(50L, TODAY, TODAY.plusDays(31), null))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void closableOnlyForApprovedAppointmentsThatAlreadyStarted() {
        Appointment started = MyAppointmentsServiceTest.appointment(1L, 7L, AppointmentStatus.APPROVED, TODAY.atTime(9, 0));
        Appointment future = MyAppointmentsServiceTest.appointment(2L, 7L, AppointmentStatus.APPROVED, TODAY.atTime(11, 0));
        Appointment exact = MyAppointmentsServiceTest.appointment(3L, 7L, AppointmentStatus.APPROVED, TODAY.atTime(10, 0));
        java.time.LocalDateTime now = TODAY.atTime(10, 0);

        assertThat(started.closableAt(now)).isTrue();
        assertThat(exact.closableAt(now)).isTrue();
        assertThat(future.closableAt(now)).isFalse();
        assertThat(MyAppointmentsServiceTest.appointment(4L, 7L, AppointmentStatus.COMPLETED, TODAY.atTime(9, 0))
                .closableAt(now)).isFalse();
    }

    static final class AgendaAppointments extends MyAppointmentsServiceTest.InMemoryAppointments {
        Long lastProfessionalId;
        String lastLocation;
        @Override public List<Appointment> findAgenda(long professionalId, LocalDate from, LocalDate to, String locationCode) {
            lastProfessionalId = professionalId;
            lastLocation = locationCode;
            return items.stream().filter(a -> a.professionalId() == professionalId).toList();
        }
    }
}
