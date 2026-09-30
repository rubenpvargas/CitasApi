# LOOP_02 — Reprogramación con retención

## Objetivo

Verificar que una solicitud `PENDING` conserva la cita original, retiene la nueva franja y que ADMIN decide liberar o mover correctamente.

## Iteraciones

| Iteración | Acción | Evidencia | Decisión |
|---|---|---|---|
| 1 | Crear modelo de retención `reschedule_requests` y columna de hold en slots | `V4__scheduling_domain.sql`, `V6__reschedule_slot_holds.sql` | PASS de persistencia |
| 2 | Implementar solicitud y decisiones approve/reject con ownership | `SchedulingService.requestReschedule()` y `decideReschedule()` | PASS de código |
| 3 | Verificar por HTTP original, nueva franja y auditoría | Request sintética 2; cita 4 conservó estado `APPROVED` | PASS |
| 4 | Confirmar movimiento después de aprobación y liberación después de rechazo | Request 2 aprobada; request 3 rechazada, cita original conservada y slot nuevo liberado | PASS |

## Parada y escalamiento

Las rutas de aprobación y rechazo quedan validadas con datos sintéticos. Falta repetirlas desde una base limpia como parte de la E2E de release.
