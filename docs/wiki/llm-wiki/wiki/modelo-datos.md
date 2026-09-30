# Modelo de datos

## HECHO

El diseño debe llegar a 3FN: relaciones N:M con tablas puente, catálogos por
FK, atributos atómicos y sin dependencias transitivas.

### Catálogos fijos de HU-007

- `roles(id, code, name, description)` tiene clave primaria `id` y código
  único; `user_roles(user_id, role_id)` conserva la relación N:M sin repetir
  roles en el usuario.
- `appointment_statuses`, `reschedule_request_statuses` e
  `insurance_regimes` tienen clave primaria propia y `code` único. Los nombres
  y el indicador terminal dependen solamente de esa clave de catálogo.
- `locations(id, code, name, address, city, department, active)` normaliza las
  dos sedes fijas. Las entidades futuras las referenciarán por FK y no copiarán
  nombre o dirección.

La migración V3 siembra los códigos estables `USER`, `PROFESSIONAL`, `ADMIN`,
los estados del PRD, los cinco regímenes y `HIC`/`ICV`. Esto evita dependencias
transitivas y valores de texto divergentes en tablas transaccionales.

## PREGUNTA ABIERTA

El modelo implementado en Flyway V4-V6 agrega EPS/planes/afiliaciones,
especialidades, profesionales y sus dos relaciones N:M, bloques, slots
atómicos, citas, historial y reprogramaciones. `professional_slots` contiene
la reserva comprometida y `reschedule_request_id` la retención provisional;
esto permite conservar la franja original durante `PENDING` y moverla de forma
atómica al aprobar.

Los índices de consulta cubren paciente-fecha, profesional-fecha, estado,
bloque-fecha, slot-inicio y solicitudes por estado. V5 contiene únicamente
usuarios y oferta sintéticos para demo.

Fuente: [requisitos 3FN](../raw/requisitos-normalizacion-3fn.md).
