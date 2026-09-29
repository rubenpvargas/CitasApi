# WF-002 — Notificación por cambio de estado

**Trigger:** Webhook recibido desde `citas-api`.

Eventos mínimos:
- cita especializada APPROVED/REJECTED;
- reprogramación APPROVED/REJECTED;
- cancelación.

## Requisitos
- validar payload mínimo;
- ramificar por tipo/estado;
- Gmail con mensaje coherente;
- respuesta webhook determinista;
- error/reintento/trazabilidad;
- secretos/credenciales fuera del JSON.

## Entregable
`WF-002-status-notifications.json`.

Validación controlada: importación en n8n 2.41.3 y webhook `POST /webhook/citas-status` con evento sintético `REJECTED`; respuesta `202` determinista. Gmail permanece sin credencial versionada.
