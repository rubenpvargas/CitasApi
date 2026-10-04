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

---

## Ejecución 2 — reconciliación 2026-10-02/04 (Builder/Verifier, máximo 4 iteraciones)

**Disparador.** La ejecución 1 se apoyó en HTTP manual sobre `SchedulingService` sin pruebas
versionadas; la auditoría del 2026-10-02 halló además: sin auditoría al crear la solicitud ni
historial de cita al aprobar, decisión sin bloqueo de filas, cancelación que dejaba la
solicitud `PENDING` con su retención y UI sin flujo de reprogramación.

**Meta verificable.** Reglas innegociables del prompt con evidencia automatizada en ambos
repos y repetición desde base limpia. **Roles:** Builder = agentes backend y frontend (cada
uno en su repo, dentro de HU-021/022 y HU-020 por la interacción con la cancelación).
Verifier = orquestador (no implementa). **Escalamiento:** migración no prevista → consulta
humana antes de continuar.

| Iteración | Builder | Diff | Pruebas API / web | Verifier | Feedback / escalamiento |
|---|---|---|---|---|---|
| 0 | — (auditoría) | — | sin pruebas de reprogramación | **FAIL** | Faltan auditoría, bloqueo, cierre en cancelación y UI. |
| 1 | Contrato Ola E y plan de migración `reschedule_request_history` + FK de retención + fuente `PROFESSIONAL` | `contratos-rest.md` (`7db6f21`) | — | **ESCALADO/APROBADO** | Migración no prevista: decidida por el orquestador como arquitecto dentro del plan aprobado por el usuario (ejecutar la guía) y documentada antes de implementar. |
| 2 | Solicitud con retención y conservación de la original; cancelación cierra la solicitud; UI de solicitud (mismo profesional/especialidad) | API `1588ed5` (V9), `581ffa8`; web `63f99b5` | `RescheduleRequestIT` (incluida carrera por la misma franja), `CancelAppointmentIT.ca01`; web `appointments-reschedule.spec` (6). Verificación `581ffa8`: 140 + 94 | **FAIL** | Decisión ADMIN todavía en código heredado: sin bloqueo ni historial. |
| 3 | Decisión hexagonal: bloqueo de solicitud y cita; aprobar mueve y libera; rechazar exige motivo y libera retención; historial ADMIN en ambas; UI distingue estados y compara franjas | API `1f0b78b`, `6dc7a47`; web `39d1012`, `576e351` | `RescheduleDecisionTest`, `RescheduleDecisionServiceTest` rl1–rl3, `RescheduleDecisionIT`; web `inbox-reschedule.spec`. Verificación independiente `fd19712`: 178 + 111, 0 fallos; web 268 specs. E2E Playwright sobre stack simulado desde base limpia: "reprogramación solicitada y aprobada" PASS; evento `RESCHEDULE_APPROVED` entregado a WF-002 simulado | **PASS** | — |

**Parada.** PASS en la iteración 3 de 4. Queda cerrada la deuda de la ejecución 1 ("falta
repetir desde base limpia"): la simulación del stack partió de una base vacía (V1→V10).
