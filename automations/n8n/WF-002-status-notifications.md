# WF-002 — Notificación por cambio de estado

**Trigger:** Webhook `POST /webhook/citas-status` invocado por `citas-api` desde su outbox
transaccional.

**Flujo:** `Webhook (Header Auth X-Webhook-Secret) → validar evento → ¿válido? → Gmail → 202`;
inválido → `400 INVALID_EVENT`; fallo de Gmail tras reintentos → `502 DELIVERY_FAILED` (el backend
reintenta con backoff y marca `FAILED` tras 3 intentos).

## Eventos
`APPOINTMENT_APPROVED`, `APPOINTMENT_REJECTED` (incluye motivo), `APPOINTMENT_CANCELLED`,
`RESCHEDULE_APPROVED`, `RESCHEDULE_REJECTED`.

Payload: `{eventId, correlationId, type, occurredAt, appointmentId, status, startAt,
recipient:{firstName, email}, reason?}`. Nunca JWT, contraseña ni documento; campos extra se
descartan.

## Decisiones
- El webhook rechaza peticiones sin la cabecera secreta (autenticación nativa del nodo).
- Un fallo de n8n no invalida la transacción de la cita: el evento queda en
  `notification_outbox` y se reintenta.
- La respuesta incluye `eventId` y `correlationId` para trazabilidad.

## Validación
`node automations/n8n/validate-workflows.mjs` (estructura, secretos y prueba de escritorio de los
cinco tipos, tipo desconocido, payload incompleto y filtrado de campos sensibles).
