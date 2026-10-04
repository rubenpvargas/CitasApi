---
id: HU-016
tipo: historia-de-usuario
titulo: Reservar cita general
estado: Completada
epica: "[[EP-004-ciclo-de-vida-de-citas]]"
esfuerzo: Alto
sprint_sugerido: Incremento 4
dependencias: ["[[HU-015-consultar-disponibilidad]]"]
relacionadas: ["[[HU-019-consultar-mis-citas]]", "[[HU-020-cancelar-cita]]"]
---
# HU-016 — Reservar cita general
## Historia de usuario
**COMO** USER **QUIERO** confirmar una cita de Medicina General con profesional y franja disponibles **PARA** obtener atención aprobada inmediatamente.
## Alcance
- Selección Medicina General, profesional, franja y creación `APPROVED`.
## Fuera de alcance
- Aprobación ADMIN o especialidades especializadas.
## Reglas de negocio
- Revalidar al confirmar; no doble reserva; estado automático `APPROVED`; auditoría fuente SYSTEM/USER según contrato.
## Dependencias y relaciones
- Épica: [[EP-004-ciclo-de-vida-de-citas]]; depende de [[HU-015-consultar-disponibilidad]].
## Esfuerzo
**Nivel:** Alto. **Justificación:** reserva atómica, concurrencia y transición auditada.
## Tareas de desarrollo
- [x] **T-01 — Definir contrato de confirmación/idempotencia y errores de conflicto**. Dificultad: Alto.
- [x] **T-02 — Implementar reserva atómica, estado y auditoría**. Dificultad: Alto.
- [x] **T-03 — Integrar confirmación UI y conflicto de franja**. Dificultad: Medio.
- [x] **T-04 — Probar doble intento concurrente y estado APPROVED**. Dificultad: Alto.
## Criterios de aceptación
### CA-01 — Reserva general
**Dado** Medicina General y una franja aún disponible **cuando** USER confirma **entonces** se crea cita `APPROVED` sin intervención ADMIN.
### CA-02 — Conflicto
**Dado** franja ocupada entre búsqueda y confirmación **cuando** USER confirma **entonces** no se crea cita y se informa conflicto seguro.
### CA-03 — Auditoría
**Dado** cita creada **cuando** se consulta su trazabilidad interna **entonces** existe transición con actor/fuente, fecha y estado.
## Definition of Done
- [x] CA-01 a CA-03 validados, incluida prueba relevante de concurrencia.
- [x] Persistencia/Flyway representa cita, slots y auditoría sin duplicar catálogos.
- [x] Contrato backend/frontend y manejo UI de conflicto están verificados.
## Evidencia de validación
| Elemento | Resultado | Evidencia | Observación |
|---|---|---|---|
| CA-01 | Conforme | `BookingIT`, `BookingSlotsTest` bk1: `POST /appointments/general` → 201 `APPROVED` sin ADMIN; E2E Playwright "reserva general → mis citas → cancelar" en verde sobre stack simulado (nginx + API + MySQL nuevo) | API `398268a`; web `2a25c53`, `7455ceb` |
| CA-02 | Conforme | `BookingIT.concurrentBookingsOfTheSameSlotProduceExactlyOne201AndOne409`: exactamente un 201 y un 409 `SLOT_UNAVAILABLE`; UI recarga disponibilidad (`booking-confirm.spec` › 409) | Bloqueo `SELECT … FOR UPDATE` de slots consecutivos |
| CA-03 | Conforme | Historial de creación con actor y fuente `USER` (IT) | — |
| DoD | Conforme | Prueba de escritorio de la transacción y la carrera en PRUEBAS_DE_ESCRITORIO.md; LOOP_01 PASS; solo `USER` reserva (403 otros) | Verificación independiente `fd19712`: 178 + 111 |

## Historial de validación
- 2026-09-17 — Creada en `Pendiente de aprobación`.
- 2026-10-04 — Validada con IT concurrente, prueba de escritorio y E2E sobre stack simulado (API `398268a`; web `7455ceb`).

## Notas y decisiones
- La estrategia exacta de bloqueo se define sin alterar RN-01.
