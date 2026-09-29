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
