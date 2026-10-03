// Validación sin n8n de los workflows exportados (Node >= 20):
//   node automations/n8n/validate-workflows.mjs
// 1. JSON válido, nodos únicos y conexiones hacia nodos existentes.
// 2. Sin secretos: ids de credencial vacíos y sin patrones de token/clave.
// 3. Prueba de escritorio de los nodos Code con eventos sintéticos.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = path.dirname(fileURLToPath(import.meta.url));
let failures = 0;
const check = (ok, message) => {
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${message}`);
  if (!ok) failures++;
};

const load = (file) => {
  const raw = fs.readFileSync(path.join(dir, file), 'utf8');
  return { raw, wf: JSON.parse(raw) };
};

function structural(file) {
  const { raw, wf } = load(file);
  const names = wf.nodes.map((n) => n.name);
  check(new Set(names).size === names.length, `${file}: nombres de nodo únicos`);
  const targets = Object.entries(wf.connections).flatMap(([from, c]) => {
    check(names.includes(from), `${file}: origen de conexión "${from}" existe`);
    return c.main.flat().map((t) => t.node);
  });
  check(targets.every((t) => names.includes(t)), `${file}: destinos de conexión existen`);
  const creds = wf.nodes.flatMap((n) => Object.values(n.credentials ?? {}));
  check(creds.every((c) => c.id === ''), `${file}: credenciales sin id ni valor versionado`);
  const secretPatterns = [/eyJ[\w-]{10,}\.eyJ[\w-]{10,}/, /AKIA[0-9A-Z]{16}/, /ya29\.[\w-]{20,}/, /"(password|secret|apiKey|token)"\s*:\s*"[^"]+"/i];
  check(!secretPatterns.some((p) => p.test(raw)), `${file}: sin tokens, claves ni contraseñas`);
  check(wf.active === false, `${file}: exportado inactivo`);
  return wf;
}

const runCode = (wf, nodeName, items) => {
  const node = wf.nodes.find((n) => n.name === nodeName);
  return new Function('items', node.parameters.jsCode)(items);
};

// WF-001
const wf1 = structural('WF-001-appointment-reminders.json');
const api = wf1.nodes.find((n) => n.type === 'n8n-nodes-base.httpRequest');
check(api.parameters.url.includes('/api/v1/automation/appointments/reminders'), 'WF-001: usa la ruta de automatización de mínimo privilegio');
check(api.parameters.genericAuthType === 'httpHeaderAuth', 'WF-001: autentica con cabecera (X-Automation-Key)');
const reminders = runCode(wf1, 'Validate minimal payload', [
  { json: { appointmentId: 1, startAt: '2026-10-06T08:00:00', locationName: 'HIC', specialtyName: 'Medicina General', professionalName: 'Prof Demo', recipient: { firstName: 'Ana', email: 'ana@example.test' } } },
  { json: { appointmentId: 2, startAt: '2026-10-06T09:00:00', recipient: {} } },
]);
check(reminders.length === 1 && reminders[0].json.email === 'ana@example.test', 'WF-001: descarta ítems sin destinatario');
check(!('documentNumber' in reminders[0].json) && !('phone' in reminders[0].json), 'WF-001: solo campos mínimos');

// WF-002
const wf2 = structural('WF-002-status-notifications.json');
const hook = wf2.nodes.find((n) => n.type === 'n8n-nodes-base.webhook');
check(hook.parameters.authentication === 'headerAuth', 'WF-002: webhook exige cabecera secreta (X-Webhook-Secret)');
const event = {
  eventId: '6f1c2e0a-0000-4000-8000-000000000001', correlationId: 'corr-1', type: 'APPOINTMENT_REJECTED',
  occurredAt: '2026-10-05T10:00:00', appointmentId: 7, status: 'REJECTED', startAt: '2026-10-06T08:00:00',
  recipient: { firstName: 'Ana', email: 'ana@example.test' }, reason: 'Orden sintética vencida',
};
const ok = runCode(wf2, 'Validate event', [{ json: { body: event } }])[0].json;
check(ok.valid === true && ok.subject.startsWith('Solicitud de cita rechazada'), 'WF-002: REJECTED válido arma asunto');
check(ok.message.includes('Orden sintética vencida'), 'WF-002: incluye motivo de rechazo');
for (const type of ['APPOINTMENT_APPROVED', 'APPOINTMENT_CANCELLED', 'RESCHEDULE_APPROVED', 'RESCHEDULE_REJECTED']) {
  const r = runCode(wf2, 'Validate event', [{ json: { body: { ...event, type, reason: undefined } } }])[0].json;
  check(r.valid === true, `WF-002: ${type} aceptado`);
}
const unknown = runCode(wf2, 'Validate event', [{ json: { body: { ...event, type: 'PASSWORD_CHANGED' } } }])[0].json;
check(unknown.valid === false && unknown.errors.includes('type'), 'WF-002: tipo desconocido rechazado (400)');
const missing = runCode(wf2, 'Validate event', [{ json: { body: { type: 'APPOINTMENT_APPROVED' } } }])[0].json;
check(missing.valid === false && missing.errors.includes('recipient.email'), 'WF-002: payload incompleto rechazado (400)');
const leak = runCode(wf2, 'Validate event', [{ json: { body: { ...event, accessToken: 'x', password: 'y' } } }])[0].json;
check(!('accessToken' in leak) && !('password' in leak), 'WF-002: campos sensibles no se propagan');

console.log(failures === 0 ? '\nOK: workflows válidos' : `\n${failures} verificación(es) fallida(s)`);
process.exit(failures === 0 ? 0 : 1);
