package com.fcv.citas.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcv.citas.domain.model.Appointment;
import com.fcv.citas.domain.model.PendingReschedule;

import java.time.LocalDateTime;

/** AppointmentDto del contrato (olas D/E); cancellable y reschedulable los calcula el backend. */
public record AppointmentResponse(long id, String status, String locationCode, String locationName, long professionalId,
                                  String professionalName, long specialtyId, String specialtyName,
                                  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startAt,
                                  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endAt,
                                  int durationMinutes, String reason, String rejectionReason,
                                  PendingRescheduleResponse pendingReschedule, boolean cancellable,
                                  boolean reschedulable) {

    static AppointmentResponse from(Appointment a, LocalDateTime now) {
        return new AppointmentResponse(a.id(), a.status().name(), a.locationCode(), a.locationName(), a.professionalId(),
                a.professionalName(), a.specialtyId(), a.specialtyName(), a.startAt(), a.endAt(), a.durationMinutes(),
                a.reason(), a.rejectionReason(), PendingRescheduleResponse.from(a.pendingReschedule()),
                a.cancellableAt(now), a.reschedulableAt(now));
    }

    public record PendingRescheduleResponse(long id,
                                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedStartAt,
                                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime requestedEndAt,
                                            String locationCode) {
        static PendingRescheduleResponse from(PendingReschedule p) {
            return p == null ? null
                    : new PendingRescheduleResponse(p.id(), p.requestedStartAt(), p.requestedEndAt(), p.locationCode());
        }
    }
}
