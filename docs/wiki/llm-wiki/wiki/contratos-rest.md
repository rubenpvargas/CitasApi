# Contrato REST

## DECISIÓN — Autenticación v1

El incremento backend aprobado HU-001 a HU-003 fija estos endpoints públicos:

- `POST /api/v1/auth/register`: registra un USER y devuelve `201` sin password.
- `POST /api/v1/auth/login`: autentica email/password y devuelve access y refresh JWT.
- `POST /api/v1/auth/refresh`: rota un refresh válido y devuelve un nuevo par.
- `POST /api/v1/auth/logout`: revoca el refresh activo y devuelve `204`.

Los errores usan Problem Details con un `code` estable. Credenciales inválidas
comparten `INVALID_CREDENTIALS`; refresh inválido, vencido, revocado o
reutilizado comparte `INVALID_REFRESH_TOKEN`. El frontend consumirá este
contrato directamente, pero su implementación no pertenece a este incremento.

Fuente: aprobación explícita del usuario de HU-001, HU-002 y HU-003 el
2026-09-17; evidencia prevista en pruebas REST de `citas-api`.

## PLAN CROSS-REPO — identidad visible tras login

**Cambio aditivo de contrato.** `POST /api/v1/auth/login` incorporará `user`
en su respuesta: `id`, `firstName`, `lastName`, `email` y `roles`. Se mantienen
los campos existentes de tokens y expiraciones. `refresh` conserva su respuesta
de tokens, pues no altera el perfil de la sesión ya conocida por el cliente.

- **citas-api:** modelo de sesión autenticada, respuesta REST de login y pruebas
  de aplicación/controlador.
- **citas-web:** tipos del cliente, cliente REST y estado visible de paciente.
- **Evidencia:** las pruebas Maven y el build Angular pasan; un login válido
  muestra el nombre devuelto por la API y uno inválido conserva el error seguro.

**Validado el 2026-09-24.** La respuesta de login incluye `user`; la interfaz
actualiza el estado visible con `firstName` y `lastName` de esa respuesta. La
verificación con un usuario sintético mostró su nombre en el dashboard.

## PREGUNTA ABIERTA

Los contratos de funcionalidades posteriores, paginación y política global de
fechas/zona horaria siguen pendientes de aprobación.

## PLAN CROSS-REPO — catálogos fijos (HU-007)

**Cambio aditivo de contrato.** La API incorporará `GET /api/v1/catalogs` para
un consumidor autenticado. La respuesta agrupa listas de solo lectura de
`roles`, `appointmentStatuses`, `rescheduleRequestStatuses`,
`insuranceRegimes` y `locations`. Cada elemento lleva su identificador técnico
estable (`code`) y su nombre; los estados incluyen `terminal` y las sedes
incluyen su ubicación pública del laboratorio. No se incorporarán rutas de
escritura para estos catálogos.

- **citas-api:** migración Flyway con tablas/semillas e idempotencia, puerto de
  consulta, adaptador de persistencia, caso de uso, controlador REST y pruebas.
- **citas-web:** no requiere modificación en esta HU; aún no existe una pantalla
  que consuma estos catálogos. Un incremento posterior usará este contrato sin
  introducir BFF.
- **Evidencia prevista:** prueba de aplicación para el mapeo de catálogos,
  `mvn test`, migración aplicada y consultas HTTP que prueben `401` sin sesión
  y `200` con JWT, sin operaciones CRUD de escritura.

**Validado el 2026-09-24.** V3 quedó aplicada en MySQL local. `GET
`/api/v1/catalogs` devuelve los cinco catálogos con JWT; sin JWT devuelve `401`
y un `POST` a la misma ruta devuelve `405`. No se cambió `citas-web` porque no
hay consumidor de catálogos en esta HU.

## CONTRATO — Vertical funcional S2-S6

La aplicación mantiene REST directo Angular → Spring Boot. Los endpoints de
perfil y citas usan el `sub` del access JWT como ownership; los endpoints
`/admin/**` exigen `ADMIN` y `/professional/**` exigen `PROFESSIONAL`.

| Capacidad | Endpoint principal | Resultado |
|---|---|---|
| Recuperación | `POST /api/v1/auth/password-reset/request`, `POST /api/v1/auth/password-reset/confirm` | Token de un solo uso; en desarrollo puede devolverse en respuesta controlada |
| Perfil/afiliación | `GET/PATCH /api/v1/me`, `GET/PUT /api/v1/me/affiliations` | Solo el usuario autenticado |
| Oferta ADMIN | `/api/v1/admin/eps`, `/plans`, `/specialties`, `/professionals` | CRUD lógico; referencias se desactivan |
| Agenda | `POST/PATCH/DELETE /api/v1/professional/blocks`, `GET /professional/calendar` | Bloques futuros, sin solapes ni sedes no asignadas |
| Disponibilidad | `GET /api/v1/availability` | Slots libres filtrables; 60 minutos exige slots consecutivos |
| Citas | `POST /appointments/general`, `POST /appointments/specialized`, `GET /appointments`, `POST /appointments/{id}/cancel` | General `APPROVED`; especializada `REQUESTED`; operación transaccional |
| Decisiones | `GET /admin/inbox`, `POST /admin/appointments/{id}/decision` | Rechazo exige motivo y libera slots |
| Reprogramación | `POST /appointments/{id}/reschedule`, `POST /admin/reschedules/{id}/decision` | La franja original permanece hasta decisión; nueva franja queda retenida |
| Operación | `GET /professional/agenda`, `POST /professional/appointments/{id}/close` | Solo agenda propia y cierre `COMPLETED`/`NO_SHOW` aplicable |

Errores de regla de negocio son `409 application/problem+json` con `code`
estable (`SLOT_UNAVAILABLE`, `REJECTION_REASON_REQUIRED`, `INVALID_TRANSITION`,
`PAST_BLOCK`, entre otros). Seguridad conserva `401/403` y no devuelve
credenciales ni tokens en mensajes de error.
### S2–S6 — sede normalizada para configuración ADMIN

`GET /api/v1/admin/locations` (`ADMIN`) devuelve `id`, `code`, `name`, dirección, ciudad, departamento y estado. Se usa para enviar `locationIds` en `PUT /api/v1/admin/professionals/{id}/capabilities`; el catálogo público continúa exponiendo solo la representación de lectura.

## PLAN CROSS-REPO — Reconciliación Ola A (HU-001 a HU-004, HU-007) — 2026-10-02

Auditoría del 2026-10-02: los endpoints de identidad existen, pero HU-004 expone
el token de recuperación de forma incondicional y permite enumerar cuentas, y
la UI no renueva sesión ni protege rutas. Cambios acordados:

**Errores (transversal, aditivo).** Problem Details con `code` estable y estado
semántico: `400 VALIDATION_ERROR`/`INVALID_REQUEST` (cuerpo o fecha mal
formada), `401` sin sesión o JWT inválido/expirado, `403 FORBIDDEN` por rol u
ownership, `404 NOT_FOUND`, `409` para reglas de negocio. Nunca `500` por
entrada del cliente.

**HU-004 — recuperación (cambio de comportamiento).**
- `POST /api/v1/auth/password-reset/request` `{email}` → `202` con cuerpo
  idéntico exista o no la cuenta. Un token nuevo invalida los anteriores del
  usuario. El token solo se almacena como hash, expira y es de un solo uso.
- Solo con `app.password-reset.expose-development-token=true` (perfil local,
  por defecto `false`) la respuesta incluye `developmentToken`; con la bandera
  en `false` nunca se devuelve ni se registra.
- `POST /api/v1/auth/password-reset/confirm` `{token,newPassword}` → `204`;
  token vencido/usado/desconocido → `409 INVALID_RESET_TOKEN` sin alterar la
  credencial. El consumo es atómico (bloqueo de fila).
- **citas-web:** la pantalla de restablecimiento lee el token de `?token=` en
  la URL (`/restablecer?token=…`); en desarrollo, si llega `developmentToken`,
  se ofrece el enlace equivalente. Mensaje neutro tras solicitar.

**HU-002/003 — sesión en citas-web (sin cambio REST).** Rutas reales con guard
de autenticación y de rol (`USER`, `PROFESSIONAL`, `ADMIN`); sesión restaurada
desde `sessionStorage`; ante `401` el interceptor llama una vez a
`/auth/refresh`, reintenta y, si falla, limpia la sesión y vuelve a login; ante
`403` muestra estado no autorizado. La autoridad sigue en el backend.

**Evidencia esperada.** citas-api: pruebas unitarias + MockMvc/IT de
registro (`201`, `400`, `409`), login, `401/403` por rol, refresh/logout y
recuperación (no enumeración, expiración, un solo uso). citas-web: specs de
interceptor, guards, servicio de auth y pantallas; `lint`, `test` y `build` en
verde.

## PLAN CROSS-REPO — Ola B (HU-005, HU-008, HU-009, HU-006) — 2026-10-02

Auditoría: endpoints existentes sin pruebas; `GET /admin/specialties` sin rol;
el USER no tiene lectura de EPS/planes ni especialidades activas; volver a una
afiliación previa viola `uk_user_affiliation`; la UI de perfil es local y no
existen pantallas de afiliación ni de catálogos ADMIN.

**HU-005 — perfil (`USER`, ownership por `sub`).**
- `GET /api/v1/me` → `200 {id, firstName, lastName, email, documentType,
  documentNumber, phone, roles}`. Nunca hash, tokens ni ids de otros usuarios.
- `PATCH /api/v1/me` `{firstName, lastName, phone}` → `200` perfil
  actualizado; `400 VALIDATION_ERROR` sin modificar datos. Email y documento
  no son editables en v1 (se preserva unicidad sin flujo de verificación).

**HU-008 — EPS/planes (`ADMIN`).** `GET /admin/eps` (incluye inactivas),
`POST /admin/eps {code,name}` → `201`, `PATCH /admin/eps/{id} {name,active}`
→ `200`; `GET /admin/plans?epsId=`, `POST /admin/eps/{epsId}/plans
{regimeId,code,name}` → `201`, `PATCH /admin/plans/{id} {name,active}` →
`200`. Sin borrado físico: retirar = `active=false`. `404 NOT_FOUND`, `409
DUPLICATE_CODE`, `403` para otros roles.

**HU-009 — especialidades.** `GET/POST/PATCH /admin/specialties` exigen
`ADMIN` (`POST` → `201`). `durationMinutes ∈ {30,60}` (si no, `400
VALIDATION_ERROR`); a lo sumo una especialidad general activa (`409
GENERAL_SPECIALTY_CONFLICT`); sin borrado físico. Las citas ya creadas
conservan su `scheduled_end_at`; la duración vigente aplica a nuevas
reservas/reprogramaciones.

**Lectura para USER (aditivo, autenticado).**
- `GET /api/v1/specialties` → especialidades activas `{id, code, name,
  durationMinutes, general}`.
- `GET /api/v1/insurance/eps` → EPS activas con sus planes activos `{id, code,
  name, plans:[{id, code, name, regime:{code,name}}]}`.

**HU-006 — afiliación (`USER`).** `GET /api/v1/me/affiliations` → afiliación
vigente (o `[]`) con EPS, plan y régimen por referencia. `PUT
/api/v1/me/affiliations {planId, membershipNumber}` → `200`; la previa queda
`is_current=false`; reelegir una combinación previa reactiva esa fila. Plan o
EPS inactivos → `409 CATALOG_INACTIVE`; plan inexistente → `404`; ante error
se conserva la afiliación previa (transacción).

**citas-web.** Perfil editable con `GET/PATCH /me`; selector EPS → plan con
estados carga/vacío/error/éxito; pantallas ADMIN de EPS/planes y
especialidades (lista, crear, editar, activar/desactivar, duración 30/60).

**Evidencia esperada.** IT de ownership, validación, `403` por rol, integridad
EPS-plan, reactivación de afiliación, una sola general; specs Angular de
servicios y pantallas.

## DECISIÓN — Tiempo y zona horaria (cierra PREGUNTA ABIERTA) — 2026-10-02

Las fechas/horas de agenda son hora local de pared de `America/Bogota`,
serializadas ISO-8601 sin offset (`YYYY-MM-DD`, `HH:mm`, `YYYY-MM-DDTHH:mm:ss`).
El backend obtiene "ahora" de un `Clock` inyectable con zona configurable
(`app.time-zone`, por defecto `America/Bogota`) para que las reglas temporales
sean deterministas en pruebas. Las consultas por rango exigen `from`/`to` con
un máximo de 31 días; no hay paginación en v1 (volúmenes sintéticos acotados).

## PLAN CROSS-REPO — Ola C (HU-010 a HU-015) — 2026-10-02

- **HU-010** `POST /admin/professionals` (`ADMIN`) → `201 {id, userId,
  firstName, lastName, email, professionalCode, licenseNumber, active}`; email
  normalizado en minúsculas; duplicados → `409 DUPLICATE_EMAIL|
  DUPLICATE_DOCUMENT|DUPLICATE_PROFESSIONAL_CODE|DUPLICATE_LICENSE`. `GET
  /admin/professionals` lista con capacidades. Nunca devuelve password/hash.
- **HU-011** `PUT /admin/professionals/{id}/capabilities {specialtyIds[≥1],
  primarySpecialtyId, locationIds[≥1], active}` → `200`. Primaria no incluida
  en `specialtyIds` → `409 PRIMARY_NOT_ASSIGNED`; especialidad/sede inactiva o
  inexistente → `409 CATALOG_INACTIVE`/`404`. Un profesional inactivo no
  publica bloques, no aparece en disponibilidad y no puede reservarse.
- **HU-012** `POST /professional/blocks {date, startTime, endTime,
  locationCode}` → `201`. Reglas: inicio > ahora; horas en `:00`/`:30`;
  duración múltiplo de 30; sin solape con ningún bloque activo del mismo
  profesional **en cualquier sede**; sede asignada y activa; profesional
  activo. Errores `409 PAST_BLOCK|BLOCK_OVERLAP|LOCATION_NOT_ASSIGNED|
  PROFESSIONAL_INACTIVE`, `400` por alineación/rango. El profesional se deriva
  del JWT (nunca del cuerpo).
- **HU-013** `PATCH /professional/blocks/{id}` (mismo cuerpo) → `200`, con
  revalidación completa de HU-012; `DELETE` → `204` (baja lógica y retiro de
  slots libres). Bloque pasado → `409 PAST_BLOCK`; con slot reservado o
  retenido por reprogramación → `409 BLOCK_COMMITTED`; ajeno → `404`.
- **HU-014** `GET /professional/calendar?from&to&locationCode?` → bloques
  propios `{id, date, startTime, endTime, locationCode, locationName,
  totalSlots, committedSlots, editable}`; sin datos de pacientes.
- **HU-015** `GET /api/v1/availability?specialtyId&from&to&locationCode?&
  professionalId?` (autenticado) → `[{professionalId, professionalName,
  specialtyId, specialtyName, locationCode, locationName, startAt, endAt,
  durationMinutes}]`. Solo inicios futuros con `duración/30` slots libres
  **consecutivos dentro del mismo bloque**, especialidad activa asignada y
  activa en el profesional, profesional activo y sede asignada. Excluye slots
  reservados o retenidos. `type` general/especializada se deriva de
  `specialty.general`.
- **citas-web:** ADMIN crea profesional y edita capacidades (selector primaria
  limitado a las asignadas); PROFESSIONAL crea/edita/elimina bloques y ve
  calendario con motivos de acciones no disponibles; USER busca disponibilidad
  real (catálogos → especialidad → sede/profesional/fecha) sin inventar
  franjas.

## PLAN CROSS-REPO — Ola D (HU-016 a HU-018) — 2026-10-02

- `POST /appointments/general {professionalId, locationCode, startAt,
  reason?}` y `POST /appointments/specialized {specialtyId, professionalId,
  locationCode, startAt, reason?}` → `201 AppointmentDto` (ver Ola E). Solo
  `USER` (`403` otros roles). Revalida en la transacción: inicio futuro,
  profesional activo, especialidad activa asignada (general = la especialidad
  general activa; especializada no puede ser general), sede asignada, y bloquea
  (`SELECT … FOR UPDATE`) los N slots consecutivos libres; si no →
  `409 SLOT_UNAVAILABLE`. General nace `APPROVED`; especializada `REQUESTED`
  con slots retenidos. Historial: creación con actor USER, fuente `USER`.
- `POST /admin/appointments/{id}/decision {approve, reason}` (`ADMIN`) → `200
  AppointmentDto`; bloquea la cita; solo desde `REQUESTED` (si no `409
  INVALID_TRANSITION`); rechazo sin motivo → `409 REJECTION_REASON_REQUIRED`;
  rechazo libera slots; aprobación los conserva; historial fuente `ADMIN`.
- **Prueba obligatoria:** IT concurrente (dos hilos, mismo slot) → exactamente
  un `201` y un `409`.
- **citas-web:** confirmación con resumen; especializada muestra "Solicitud
  pendiente de aprobación"; `409` recarga disponibilidad con mensaje seguro.
  ADMIN decide con motivo obligatorio al rechazar.

## PLAN CROSS-REPO — Ola E (HU-019 a HU-022) — 2026-10-02

- `AppointmentDto {id, status, locationCode, locationName, professionalId,
  professionalName, specialtyId, specialtyName, startAt, endAt,
  durationMinutes, reason, rejectionReason, pendingReschedule:{id,
  requestedStartAt, requestedEndAt, locationCode}|null, cancellable,
  reschedulable}` — calculado por backend.
- **HU-019** `GET /appointments?status&from&to` solo propias; `GET
  /appointments/{id}` propia, ajena → `404 NOT_FOUND`.
  `rejectionReason` procede del historial `REJECTED`.
- **HU-020** `POST /appointments/{id}/cancel` → `200`; propia, futura y en
  `REQUESTED|APPROVED`; si no `409 INVALID_TRANSITION`; libera slots; cierra
  la reprogramación pendiente (estado terminal, fuente `SYSTEM`) y libera su
  retención. No existe endpoint de reactivación.
- **HU-021** `POST /appointments/{id}/reschedule {startAt, locationCode}` →
  `201 {id, appointmentId, status:"PENDING", requestedStartAt,
  requestedEndAt, locationCode}`. Solo `APPROVED` futura propia; conserva
  profesional y especialidad (no se aceptan en el cuerpo); sede asignada;
  inicio futuro con slots consecutivos libres retenidos; una sola `PENDING`
  por cita (`409 RESCHEDULE_ALREADY_PENDING`). La cita original no cambia.
- **HU-022** `POST /admin/reschedules/{id}/decision {approve, reason}` →
  `200`; bloquea solicitud y cita; solo `PENDING`; aprobar: la cita adopta
  nueva franja/sede, se liberan slots antiguos y los retenidos pasan a la
  cita; rechazar exige motivo y libera la retención.
- **Auditoría (Flyway):** tabla append-only `reschedule_request_history
  (request_id, status_id, changed_by_user_id, change_source, reason,
  changed_at)` para creación, decisión y cierre por cancelación; la cita
  registra además en `appointment_status_history` el cambio aprobado. Las
  fuentes admiten `PROFESSIONAL` (necesario para HU-024).
- **citas-web:** Mis citas con filtros estado/fecha, estados de UI completos,
  etiquetas correctas por estado, motivo de rechazo, cancelar con
  confirmación accesible, reprogramar buscando disponibilidad del mismo
  profesional/especialidad y estado pendiente visible.

## PLAN CROSS-REPO — Ola F (HU-023 a HU-025) — 2026-10-02

- **HU-023** `GET /professional/agenda?from&to&locationCode?` → solo citas
  propias `APPROVED`: `{id, startAt, endAt, locationCode, specialtyName,
  patientName, closable}`; sin documento, email ni teléfono.
- **HU-024** `POST /professional/appointments/{id}/close {outcome:
  COMPLETED|NO_SHOW}` → `200`. **"Aplicable"** = cita propia en `APPROVED`
  cuyo `startAt ≤ ahora`. Si no `409 INVALID_TRANSITION`; ajena `404`;
  historial con fuente `PROFESSIONAL`.
- **HU-025** `GET /admin/inbox?locationCode&professionalId&specialtyId&from&to`
  → `{appointments:[REQUESTED], reschedules:[PENDING]}` con datos para decidir;
  nunca incluye resueltas.
- **citas-web:** agenda con filtros día/semana/sede y cierre; bandeja ADMIN
  con filtros que enlaza a la decisión de HU-018/HU-022.

## VALIDACIÓN — Ola A backend (2026-10-02)

Implementado conforme al plan con estas precisiones (HECHO):
- Política de contraseña del dominio: 8–72 caracteres, al menos una mayúscula
  y un dígito, en `register.password` y `password-reset/confirm.newPassword`
  (`400 VALIDATION_ERROR` con `errors[]`).
- `password-reset/request` → `202 {message}`; `developmentToken` se omite
  (no `null`) salvo `PASSWORD_RESET_EXPOSE_DEV_TOKEN=true`; token base64url de
  43 caracteres, TTL 30 min (`PASSWORD_RESET_TOKEN_MINUTES`); confirmar revoca
  todas las sesiones refresh activas del usuario.
- Códigos adicionales: `401 UNAUTHORIZED`, `405 METHOD_NOT_ALLOWED`,
  `415 UNSUPPORTED_MEDIA_TYPE`, `500 INTERNAL_ERROR` (cuerpo genérico).
- Registro duplicado conserva `409 IDENTIFIER_ALREADY_REGISTERED` (no revela
  si fue email o documento).

## IMPLEMENTACIÓN — Ola B (2026-10-03, pendiente de verificación independiente)

Backend `704db7e`, `6766460`, `cf886f0`, `6197955`; web `c5ea141`…`199ce56`.
Precisiones respecto al plan (HECHO, a validar en el cierre de la ola):
- Planes ADMIN planos `{id, code, name, active, epsId, epsCode, epsName,
  regimeId, regimeCode, regimeName}`; afiliación plana; especialidad ADMIN
  incluye `requiresAdminApproval`.
- `409 DUPLICATE_NAME` (especialidad); códigos de catálogo `[A-Za-z0-9_-]+`
  almacenados en mayúsculas; `active` obligatorio en actualizaciones.
- `/me/affiliations` exige `USER`; `/me` admite cualquier rol autenticado.
- PREGUNTA ABIERTA: `GET /catalogs` no expone `id` de regímenes que
  `POST /admin/eps/{id}/plans` requiere (`regimeId`); resolver en Ola C
  (añadir `id` o aceptar `regimeCode`).
