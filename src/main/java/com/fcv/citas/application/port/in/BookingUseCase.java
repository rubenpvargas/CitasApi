package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.BookingCommand;
import com.fcv.citas.domain.model.Appointment;

/** HU-016/HU-017 — reserva de cita general (APPROVED) o solicitud especializada (REQUESTED). */
public interface BookingUseCase {
    Appointment bookGeneral(long userId, BookingCommand command);

    Appointment bookSpecialized(long userId, BookingCommand command);
}
