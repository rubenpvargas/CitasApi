# WF-001 — Recordatorio de citas próximas

**Trigger:** Schedule cada hora.

**Flujo:** `Schedule → GET /api/v1/automation/appointments/reminders?hours=24&windowMinutes=60 → validar payload mínimo → Gmail → registro de ejecución`.

## Decisiones
- **Mínimo privilegio:** la API se consulta con la cabecera `X-Automation-Key` (credencial
  *Header Auth* configurada en n8n, valor en `AUTOMATION_API_KEY` del backend). La clave solo
  sirve para esta ruta; no es un JWT de usuario ni caduca a los 15 minutos.
- **Sin duplicados:** la API devuelve solo citas `APPROVED` que empiezan en
  `[ahora + 24 h − 60 min, ahora + 24 h)`. Con disparo horario cada cita entra en una única
  ejecución. Riesgo residual: si n8n está detenido durante una ventana, ese recordatorio se pierde
  (no se reenvía para no duplicar).
- **Datos mínimos:** fecha, sede, especialidad, profesional y `recipient {firstName, email}`.
  Sin documento, teléfono ni identificadores de usuario.
- **API no disponible:** 3 intentos con 5 s de espera; Gmail con 2 intentos y continuación para
  registrar el fallo sin datos personales.
- **Credenciales fuera del JSON:** ids de credencial vacíos; se configuran al importar.

## Validación
`node automations/n8n/validate-workflows.mjs` verifica estructura, ausencia de secretos y ejecuta
la lógica del nodo de validación con datos sintéticos. Activar solo tras una ejecución manual
controlada con destinatarios sintéticos.
