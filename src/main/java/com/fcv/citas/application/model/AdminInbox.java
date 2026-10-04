package com.fcv.citas.application.model;

import com.fcv.citas.domain.model.Appointment;

import java.util.List;

/** HU-025 — pendientes de decisión: citas REQUESTED y reprogramaciones PENDING. */
public record AdminInbox(List<Appointment> appointments, List<RescheduleInboxItem> reschedules) {
}
