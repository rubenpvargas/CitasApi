package com.fcv.citas.application.port.out;

import com.fcv.citas.application.model.InboxFilter;
import com.fcv.citas.application.model.NewReschedule;
import com.fcv.citas.application.model.RescheduleInboxItem;
import com.fcv.citas.domain.model.RescheduleRequest;
import com.fcv.citas.domain.model.RescheduleStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RescheduleRepositoryPort {
    long insert(NewReschedule request, Instant at);

    Optional<RescheduleRequest> findPendingForAppointment(long appointmentId);

    /** Bloquea la fila de la solicitud hasta el fin de la transacción. */
    Optional<RescheduleRequest> lockById(long id);

    void updateStatus(long id, RescheduleStatus status, Long decidedByUserId, String reason, LocalDateTime decidedAt);

    /** Historial append-only (reschedule_request_history). */
    void addHistory(long id, RescheduleStatus status, Long actorUserId, String source, String reason, Instant at);

    List<RescheduleInboxItem> findPending(InboxFilter filter);
}
