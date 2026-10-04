# Pruebas de escritorio — reglas críticas

Trazas manuales de los algoritmos y flujos que deciden disponibilidad, reserva y
estados. Cada caso se automatiza en la prueba indicada; la traza documenta el
razonamiento y la prueba demuestra que el código lo cumple. Datos sintéticos.
Hora de negocio: `America/Bogota` (Clock inyectable).

## HU-012 — Validación de bloques (`BlockValidator.validate`)

"Ahora" = 2026-10-05 10:00. Orden de verificación: alineación → fin > inicio →
inicio > ahora → solape con cualquier bloque activo del profesional.

| # | Entrada (fecha, inicio–fin, existentes) | Traza | Salida | Prueba |
|---|---|---|---|---|
| 1 | 10-06, 08:00–09:00, ninguno | 0 %30=0 ✓; 09:00>08:00 ✓; 10-06T08:00 > ahora ✓; sin bloques | válido | `BlockValidatorTest.dc1ValidFutureBlockOnAnotherDayHasNoViolation` |
| 2 | 10-05, 10:30–12:00 | alineado ✓; 12:00>10:30 ✓; 10:30 > 10:00 ✓ | válido (mismo día futuro) | `dc2SameDayFutureBlockIsAllowed` |
| 3 | 10-05, 10:00–11:00 | forma ✓; ¿10:00 > 10:00? no | `PAST_BLOCK` (409) | `dc3StartAtOrBeforeNowIsPastBlock` |
| 4 | 10-06, 08:15–09:00 | 15 %30=15 ≠ 0 | `START_NOT_ALIGNED` (400, campo `startTime`) | `dc4MisalignedTimesAreRejectedBeforeTemporalRules` |
| 5 | 10-06, 09:00–09:00 | ¿09:00 > 09:00? no | `END_NOT_AFTER_START` (400, campo `endTime`) | `dc5EndMustBeAfterStart` |
| 6 | 10-06, 09:30–11:00 en HIC; existe 08:00–10:00 en **ICV** | misma fecha; 09:30<10:00 ✓ y 08:00<11:00 ✓ (la sede no se considera) | `BLOCK_OVERLAP` (409) | `dc6OverlapWithAnActiveBlockInAnyLocationIsRejectedButTouchingIsNot` |
| 6b | 10-06, 10:00–11:00, mismo existente | ¿10:00 < 10:00? no → contiguo, sin solape | válido | misma prueba |

## HU-012 — Discretización en slots de 30 min (`BlockSchedule.slots`)

Ciclo: `cursor = inicio`; mientras `cursor + 30 ≤ fin` añadir `[cursor, cursor+30)` y avanzar 30.

| # | Bloque | Traza | Salida | Prueba |
|---|---|---|---|---|
| 1 | 08:00–09:00 | 08:30≤09:00 añade; 09:00≤09:00 añade; 09:30>09:00 para | [08:00–08:30], [08:30–09:00] | `BlockScheduleTest.ds1OneHourBlockProducesTwoConsecutiveSlots` |
| 2 | 14:30–15:00 | 15:00≤15:00 añade; 15:30>15:00 para | 1 slot | `ds2MinimalBlockProducesOneSlot` |
| 3 | 08:00–12:00 | 8 iteraciones; último cursor 11:30 | 8 slots, último [11:30–12:00] | `ds3MorningBlockProducesEightSlotsAndLastEndsAtBlockEnd` |
| 4 | 23:00–23:30 | 23:30≤23:30 añade; 00:00 del día siguiente > fin | 1 slot, sin cruzar medianoche | `ds4BlockEndingAtLastHalfHourOfTheDayDoesNotCrossMidnight` |
| 5 | 08:00–09:00 HIC persistido | lectura MySQL de `start_at` | `T08:00`, `T08:30` (sin desfase de 5 h) | `AvailabilityBlockIT.ca01PublishesBlockAndStoresSlotsAtTheSameWallClockTime` |

## HU-015 — Inicios con slots consecutivos (`AvailabilityCalculator.calculate`)

`n = duración/30`. Por bloque se ordenan los inicios libres; el índice `i`
califica si `inicio[i] > ahora` y `inicio[i+k] = inicio[i] + 30·k` para `k < n`.
Día 10-06; ahora = 10-05 10:00 salvo indicación.

| # | Slots libres (bloque) y duración | Traza | Salida | Prueba |
|---|---|---|---|---|
| 1 | 08:00–09:00, libres {08:00, 08:30}, 60 | n=2; i=0: 08:30 = 08:00+30 ✓; i=1: fuera de rango | [08:00–09:00] | `AvailabilityCalculatorTest.dc1OneHourBlockOffersExactlyOneSixtyMinuteStart` |
| 2 | 08:00–10:00 con 08:30 ocupado → {08:00, 09:00, 09:30}, 60 | i=0: 09:00 ≠ 08:30 ✗; i=1: 09:30 = 09:00+30 ✓ | [09:00–10:00] | `dc2BookedMiddleSlotBreaksConsecutivenessForSixtyButNotForThirty` |
| 2b | mismos slots, 30 | n=1: todo slot futuro califica | 08:00, 09:00, 09:30 | misma prueba |
| 3 | 08:00–09:30, {08:00, 08:30, 09:00}, 60 | i=0 ✓; i=1 ✓; i=2 requeriría 09:30 fuera del bloque | [08:00–09:00], [08:30–09:30] | `dc3LastSlotOfTheBlockCannotStartASixtyMinuteAppointment` |
| 4 | bloque A {08:00}, bloque B {08:30}, 60 | se agrupa por bloque; cada uno tiene 1 < 2 | [] (no se combinan bloques) | `dc4ConsecutiveSlotsOfDifferentBlocksDoNotCombine` |
| 5 | ahora = 10-06 08:30, {08:00…09:30}, 60 | i=0 y i=1 no son > ahora; i=2: 09:00 ✓ y 09:30 ✓ | [09:00–10:00] | `dc5StartsAtOrBeforeNowAreExcluded` |
| 6 | lista vacía o duración 45 | guarda temprana | [] | `dc6NoFreeSlotsOrInvalidDurationOfferNothing` |

`AvailabilityQueryIT` reproduce el caso 2 de extremo a extremo con una retención
de reprogramación en 08:30: solo devuelve `T09:00:00`.

## HU-015/016/017 — Flujo de reserva en la UI (`booking.ts`)

| # | Acción | Estado del componente | HTTP | Pantalla esperada | Spec |
|---|---|---|---|---|---|
| 1 | Abre `/reservar` | carga de especialidades | `GET /specialties`, `GET /catalogs` | Especialidades reales, sedes activas, sin médicos simulados | `booking.spec` › lista especialidades reales y sedes activas |
| 2 | Medicina General + HIC + fechas → Buscar | `searching=true`, botón deshabilitado | `GET /availability?specialtyId&from&to&locationCode` | "Buscando cupos disponibles…" | `booking.spec` › busca con specialtyId/from/to |
| 3 | Respuesta `[]` | `slots=[]` | — | Mensaje vacío en región `status` | `booking.spec` › resultado vacío |
| 4 | Respuesta con 3 franjas | `slots=3` | — | Solo las franjas devueltas, agrupadas por día | `booking.spec` › renderiza solo las franjas devueltas |
| 5 | Error de red | `searchError` | `GET` falla (0) | Alerta "No fue posible conectar…" | `booking.spec` › error de red al buscar |
| 6 | Rango > 31 días | `searchError` | — | "…no puede superar 31 días" | `booking.spec` › rechaza rangos inválidos |
| 7 | Confirma general | `bookingLoading=true` | `POST /appointments/general` | Botón deshabilitado, sin doble envío | `booking-confirm.spec` › evita doble envío |
| 8 | 201 `APPROVED` | cita reservada | — | "¡Cita agendada con éxito!" con datos devueltos | `booking-confirm.spec` › 201 |
| 9 | 409 `SLOT_UNAVAILABLE` | selección limpiada, recarga | `GET /availability` automático | Alerta de franja tomada; la franja desaparece | `booking-confirm.spec` › 409 SLOT_UNAVAILABLE |
| 10 | Especializada → 201 `REQUESTED` | reservada `REQUESTED` | `POST /appointments/specialized` | "Solicitud pendiente de aprobación"; nunca "Confirmada" | `booking-specialized.spec` › envía POST /appointments/specialized |
| 11 | 403/400 al confirmar | `bookingError` | `POST` → 403/400 | Mensaje en pantalla, sin redirección | `booking-confirm.spec` › 403; › 400 |

## HU-024 — Cierre de atención en la UI (`agenda.ts`)

| # | Situación | Estado | HTTP | Pantalla esperada | Spec (`agenda-close.spec`) |
|---|---|---|---|---|---|
| 1 | Ítem `closable:true` y otro `false` | agenda cargada | `GET /professional/agenda` | Botones solo en el cerrable; el otro explica que se habilita al iniciar | closable=true ofrece…; closable=false muestra el motivo |
| 2 | "Marcar atendida" → confirmar | objetivo (1, COMPLETED) | `POST …/1/close {outcome:"COMPLETED"}` | `alertdialog`; botón deshabilitado al guardar; recarga | COMPLETED: confirmación accesible y POST |
| 3 | "Marcar no asistió" → confirmar | objetivo (1, NO_SHOW) | `POST …/1/close {outcome:"NO_SHOW"}` | Diálogo "No asistió"; recarga | NO_SHOW |
| 4 | Escape / Volver | objetivo limpio | — | No se envía nada | Escape/Volver cancela sin llamar a la API |
| 5 | 409 `INVALID_TRANSITION` | error en diálogo | `POST` → 409 y recarga | "ya no está en un estado…" | 409 INVALID_TRANSITION |
| 6 | 404 (cita ajena) | error en diálogo | `POST` → 404 | "…no existe…" | 404 (cita ajena) |
| 7 | 403 | error en diálogo, sin navegación | `POST` → 403 | "No tienes permisos…" | 403 en el cierre se muestra en el diálogo sin redirigir |

## HU-016/017 — Transacción de reserva (`BookingSlots`)

Día D; ahora = 10-05 10:00. Slots libres bloqueados `id@hora/bloque`.

| Caso | Entrada | Traza | Salida | Prueba |
|---|---|---|---|---|
| bk1 | {11@08:00/b1, 12@08:30/b1}, inicio 08:00, 30 min | n=1; inicios ofertables 08:00, 08:30; coincide 08:00 | [11] | `BookingSlotsTest.bk1…` |
| bk2 | mismos, 60 min | n=2; 08:30 = 08:00+30 ✓ | [11, 12] | `bk2…`, `BookingIT.hu017…` |
| bk3 | {11@08:00}, 60 min (08:30 ocupado) | 1 libre, se necesitan 2 | [] → 409 `SLOT_UNAVAILABLE` | `bk3…` |
| bk4 | {11@08:00/b1, 21@08:30/b2}, 60 min | agrupado por bloque: 1 y 1 | [] | `bk4…` |
| bk5 | ahora = D 08:00, inicio 08:00 | 08:00 no es posterior a ahora | [] | `bk5…` |
| bk6 | inicio 08:15 o perdedor de carrera (relectura vacía) | sin inicio ofertable | [] | `bk6…` |
| carrera | dos hilos, misma franja | H1 bloquea, asigna y confirma; H2 espera el bloqueo, relee y ve `appointment_id` | un 201 y un 409; 1 fila | `BookingIT.concurrentBookings…` |

## HU-020 — Cancelación (estado × tiempo × reprogramación)

| Estado, inicio | Resultado | Prueba |
|---|---|---|
| REQUESTED +60 min / APPROVED +60 min | `CANCELLED` | `AppointmentCancelTest` |
| APPROVED, empieza ahora | `INVALID_TRANSITION` | ídem |
| REQUESTED, hace 30 min | `INVALID_TRANSITION` | ídem |
| CANCELLED / REJECTED (futura) | `INVALID_TRANSITION` | ídem |
| COMPLETED / NO_SHOW (pasada) | `INVALID_TRANSITION` | ídem |
| APPROVED futura con reprogramación PENDING | `CANCELLED`; slot propio libre; solicitud `CANCELLED` (fuente SYSTEM); retención libre; evento `APPOINTMENT_CANCELLED` | `CancelAppointmentServiceTest.ca01`, `CancelAppointmentIT.ca01` |
| Cita ajena | 404 | `CancelAppointmentIT.ca02…` |

## HU-021/022 — Ciclo de vida de la reprogramación

Tras la solicitud: S1 (original) ASIGNADO a la cita; S2 (nuevo) RETENIDO por la solicitud.

| Acción | S1 | S2 | Cita | Prueba |
|---|---|---|---|---|
| Aprobar | LIBRE | ASIGNADO a la cita | adopta sede y horario de S2; solicitud APPROVED; historial ADMIN en ambas | `RescheduleDecisionServiceTest.rl1`, `RescheduleDecisionIT.ca01` |
| Rechazar con motivo | sigue ASIGNADO | LIBRE | sin cambios; solicitud REJECTED con motivo | `rl2`, `RescheduleDecisionIT.ca02` |
| Rechazar sin motivo | sigue ASIGNADO | sigue RETENIDO | 409 `REJECTION_REASON_REQUIRED` | `rl3` |
| Segunda decisión | sin cambios | sin cambios | 409 `INVALID_TRANSITION` | `rl3`, `RescheduleDecisionIT.ca01` |
| Dos solicitudes por la misma S2 | — | RETENIDO solo por la ganadora | un 201 y un 409 | `RescheduleRequestIT.concurrent…` |

## HU-024 — Elegibilidad de cierre (`AppointmentCloseTest`, ahora = 10:00)

| Estado | Inicio | Resultado pedido | Salida |
|---|---|---|---|
| APPROVED | hace 30 min | COMPLETED | COMPLETED |
| APPROVED | ahora | NO_SHOW | NO_SHOW |
| APPROVED | en 30 min | COMPLETED | `INVALID_TRANSITION` |
| REQUESTED | hace 30 min | COMPLETED | `INVALID_TRANSITION` |
| CANCELLED | hace 30 min | NO_SHOW | `INVALID_TRANSITION` |
| COMPLETED | hace 30 min | NO_SHOW | `INVALID_TRANSITION` |
| APPROVED | hace 30 min | CANCELLED (no permitido) | `INVALID_TRANSITION` (REST responde 400 antes) |

## Ola G — Despachador del outbox (base 30 s, máximo 3 intentos, ahora T)

| Caso | Intento | ¿Entregado? | Estado | `next_attempt_at` | Prueba |
|---|---|---|---|---|---|
| ob1 | 1 | sí (2xx) | SENT | — | `OutboxRetryPolicyTest.ob1` |
| ob2 | 1 | no | PENDING | T+30 s | `ob2`; IT con base 2 s |
| ob3 | 2 | no | PENDING | T+60 s | `ob3` |
| ob4 | 3 | no | FAILED | — | `ob4`; IT 500×3 → FAILED sin cuarto envío |
| ob5 | 3 | sí | SENT | — | `ob5` |
| — | — | URL de webhook vacía | PENDING, sin envío | — | `OutboxDispatchServiceTest.disabled…` |

Simulación 2026-10-04 (stack sin Docker): con el simulador de n8n detenido, la cancelación
respondió 200 y el evento quedó `PENDING`/`IO_ERROR`; al reactivarlo pasó a `SENT` en el
intento 2.

## WF-001 — Ventana de recordatorios (`hours=H`, `windowMinutes=W`)

Una cita `APPROVED` se incluye si `inicio ∈ [ahora + H − W, ahora + H)`; con disparo
horario y `W=60` cada cita cae en una sola ejecución.

| Ahora | Cita | H, W | ¿Incluida? | Evidencia |
|---|---|---|---|---|
| 10-04 11:05 | 10-05 08:30 APPROVED (faltan 20 h 24 min) | 21, 120 → [19 h, 21 h) | sí | simulación 2026-10-04: 1 ítem, nodo de validación arma 1 correo |
| 10-04 11:05 | misma | 19, 60 → [18 h, 19 h) | no | simulación: `[]` |
| — | CANCELLED / REJECTED | cualquiera | no | simulación: solo la `APPROVED` aparece; `AutomationRemindersIT` |
| — | sin clave / clave errónea / clave en otra ruta / `hours=99` | — | 401 / 401 / 403 / 400 | simulación y `AutomationRemindersIT` |
