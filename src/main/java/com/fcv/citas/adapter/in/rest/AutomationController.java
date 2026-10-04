package com.fcv.citas.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcv.citas.application.model.ReminderItem;
import com.fcv.citas.application.port.in.ReminderQueryUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ola G (WF-001) — consumido por n8n con la cabecera X-Automation-Key (rol AUTOMATION, configurado en
 * SecurityConfig). Respuesta mínima: sin documento, teléfono, tokens ni ids de usuario.
 */
@RestController
@RequestMapping("/api/v1/automation")
public class AutomationController {
    private final ReminderQueryUseCase reminders;

    public AutomationController(ReminderQueryUseCase reminders) {
        this.reminders = reminders;
    }

    @GetMapping("/appointments/reminders")
    List<ReminderResponse> reminders(@RequestParam(defaultValue = "24") int hours,
                                     @RequestParam(defaultValue = "60") int windowMinutes) {
        return reminders.upcoming(hours, windowMinutes).stream().map(ReminderResponse::from).toList();
    }

    record Recipient(String firstName, String email) {
    }

    record ReminderResponse(long appointmentId,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startAt,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endAt,
                            String locationName, String specialtyName, String professionalName, Recipient recipient) {
        static ReminderResponse from(ReminderItem r) {
            return new ReminderResponse(r.appointmentId(), r.startAt(), r.endAt(), r.locationName(), r.specialtyName(),
                    r.professionalName(), new Recipient(r.recipientFirstName(), r.recipientEmail()));
        }
    }
}
