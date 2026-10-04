package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.ReminderItem;

import java.util.List;

/** Citas aprobadas próximas para recordatorios automatizados. */
public interface ReminderQueryUseCase {
    List<ReminderItem> upcoming(int hours);
}
