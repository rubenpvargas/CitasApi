# LOOP_01 — Reserva concurrente

## Objetivo

Verificar que dos intentos sobre la misma franja no producen doble reserva y que el segundo recibe conflicto seguro.

## Iteraciones

| Iteración | Acción | Evidencia | Decisión |
|---|---|---|---|
| 1 | Implementar bloqueo transaccional de slots con `FOR UPDATE`, índice único lógico y auditoría | `SchedulingService.book()`; `V4__scheduling_domain.sql` | PASS de implementación |
| 2 | Reserva especializada sintética y decisión ADMIN | HTTP: `REQUESTED` → `APPROVED`, id sintético 4 | PASS funcional |
| 3 | Prueba concurrente automatizada de dos clientes | `scripts/verify-concurrency.ps1`: una respuesta 2xx y una respuesta 409 sobre el mismo slot | PASS |

## Parada y feedback

El loop se detiene tras la evidencia automatizada: el bloqueo transaccional evita la doble reserva.

---

## Ejecución 2 — reconciliación 2026-10-02/03 (Builder/Verifier)

**Disparador.** Auditoría del Verifier (orquestador) el 2026-10-02: la evidencia previa
dependía de `scripts/verify-concurrency.ps1`, fuera de los repos y sin versionar, y el
código permitía doble reserva por otras vías.

**Meta verificable.** Ningún slot puede asignarse a dos citas, incluido el mismo profesional
en dos sedes; una carrera sobre la misma franja produce exactamente un `201` y un `409
SLOT_UNAVAILABLE`, demostrado por pruebas versionadas en `citas-api`.

**Roles.** Builder: agente backend (solo `citas-api`, sin UI ni contrato fuera de lo
aprobado). Verifier: orquestador, revisión separada sobre worktree aislado y base
`citas_verify`; no implementa.

**Presupuesto.** 3 iteraciones. **Escalamiento:** si tras 3 iteraciones persiste FAIL, se
detiene como BLOCKED y se consulta al usuario.

| Iteración | Builder (cambio mínimo) | Diff | Pruebas API / web | Verifier | Feedback |
|---|---|---|---|---|---|
| 0 | — (auditoría) | — | `mvn test`: 11 pruebas, ninguna de reserva | **FAIL** | Solape de bloques validado solo por sede (`location_id=?`): el mismo profesional podía tener bloques simultáneos en HIC e ICV y recibir dos citas; disponibilidad sin regla de 60 min; concurrencia sin prueba en el repo. |
| 1 | Regla de dominio `BlockValidator` con solape en cualquier sede, bloqueo de la fila del profesional al crear/editar bloques y `AvailabilityCalculator` de slots consecutivos | `0d2727b`, `ba818ef`, `1becd7c` | Unitarias `BlockValidatorTest` (RED 9/12 → GREEN), `AvailabilityCalculatorTest` (RED 5/6 → GREEN); IT `AvailabilityBlockIT`, `AvailabilityQueryIT`. Verificación independiente `028ecf8`: 109 + 79, 0 fallos | **FAIL parcial** | Bloques y disponibilidad correctos, pero la reserva seguía en `SchedulingService` con su propio `ensureConsecutive` y sin prueba concurrente versionada. |
| 2 | Reserva hexagonal: bloqueo `SELECT … FOR UPDATE` de los N slots consecutivos reutilizando la regla de dominio "dentro de un bloque"; misma protección en la retención de reprogramación | `398268a`, `581ffa8` | `BookingSlotsTest` bk1–bk6; IT `BookingIT.concurrentBookingsOfTheSameSlotProduceExactlyOne201AndOne409` y `RescheduleRequestIT.concurrentRequestsForTheSameTargetSlotProduceExactlyOne201AndOne409`. Verificación independiente `581ffa8`: 140 unitarias + 94 IT, 0 fallos. Web: 268 specs; `booking-confirm.spec` › 409 recarga disponibilidad | **PASS** | — |

**Parada.** PASS en la iteración 2 de 3. La regla queda protegida por base de datos (bloqueo
pesimista de slots) y por dominio (solape entre sedes). Riesgo residual: un paciente puede
reservar dos citas simultáneas con profesionales distintos; no es requisito del PRD y no se
implementó para no inventar reglas.
