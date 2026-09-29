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
