package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.AdminInbox;
import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-025 — bandeja ADMIN: pendientes de ambos tipos con filtros normalizados. */
class AdminInboxServiceTest {
    final FilterAppointments appointments = new FilterAppointments();
    final CancelAppointmentServiceTest.RecordingReschedules reschedules = new CancelAppointmentServiceTest.RecordingReschedules();
    final AdminInboxService service = new AdminInboxService(appointments, reschedules);

    @Test
    void ca01Ca02ReturnsRequestedAppointmentsAndPendingReschedulesWithNormalizedFilters() {
        appointments.items.add(MyAppointmentsServiceTest.appointment(1L, 7L, AppointmentStatus.REQUESTED, TODAY.atTime(8, 0)));

        AdminInbox inbox = service.inbox(new InboxFilter(" icv ", 3L, 4L, TODAY, TODAY.plusDays(7)));

        assertThat(inbox.appointments()).extracting(Appointment::id).containsExactly(1L);
        assertThat(inbox.reschedules()).isEmpty();
        assertThat(appointments.lastFilter.locationCode()).isEqualTo("ICV");
        assertThat(appointments.lastFilter.professionalId()).isEqualTo(3L);
    }

    @Test
    void invertedDateRangeIsRejected() {
        assertThatThrownBy(() -> service.inbox(new InboxFilter(null, null, null, TODAY, TODAY.minusDays(1))))
                .isInstanceOf(RequestValidationException.class).extracting("field").isEqualTo("to");
    }

    static final class FilterAppointments extends MyAppointmentsServiceTest.InMemoryAppointments {
        InboxFilter lastFilter;
        @Override public List<Appointment> findRequested(InboxFilter filter) {
            lastFilter = filter;
            return items;
        }
    }

    @SuppressWarnings("unused")
    private static LocalDate unused() {
        return TODAY;
    }
}
