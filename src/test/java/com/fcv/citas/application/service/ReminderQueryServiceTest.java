package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.ReminderItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** WF-001 sin duplicados — ventana [ahora + hours − windowMinutes, ahora + hours) con ahora = 10:00 Bogotá. */
class ReminderQueryServiceTest {
    final WindowAppointments appointments = new WindowAppointments();
    final ReminderQueryService service = new ReminderQueryService(appointments, CLOCK);

    @Test
    void windowIsHalfOpenAndEndsAtNowPlusHours() {
        service.upcoming(24, 60);

        assertThat(appointments.from).isEqualTo(TODAY.plusDays(1).atTime(9, 0));
        assertThat(appointments.to).isEqualTo(TODAY.plusDays(1).atTime(10, 0));
    }

    @Test
    void customWindowAndBounds() {
        service.upcoming(1, 15);
        assertThat(appointments.from).isEqualTo(TODAY.atTime(10, 45));
        assertThat(appointments.to).isEqualTo(TODAY.atTime(11, 0));
        service.upcoming(72, 120);
        assertThat(appointments.to).isEqualTo(TODAY.plusDays(3).atTime(10, 0));
    }

    @Test
    void outOfRangeParametersAreRejected() {
        assertThatThrownBy(() -> service.upcoming(0, 60)).isInstanceOf(RequestValidationException.class)
                .extracting("field").isEqualTo("hours");
        assertThatThrownBy(() -> service.upcoming(73, 60)).isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.upcoming(24, 14)).isInstanceOf(RequestValidationException.class)
                .extracting("field").isEqualTo("windowMinutes");
        assertThatThrownBy(() -> service.upcoming(24, 121)).isInstanceOf(RequestValidationException.class);
    }

    static final class WindowAppointments extends MyAppointmentsServiceTest.InMemoryAppointments {
        LocalDateTime from;
        LocalDateTime to;
        @Override public List<ReminderItem> findApprovedStartingBetween(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
            return List.of();
        }
    }
}
