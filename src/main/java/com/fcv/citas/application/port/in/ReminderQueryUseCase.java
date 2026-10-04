package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.ReminderItem;

import java.util.List;

/** WF-001 — citas APPROVED que empiezan en [ahora + hours − windowMinutes, ahora + hours). */
public interface ReminderQueryUseCase {
    List<ReminderItem> upcoming(int hours, int windowMinutes);
}
