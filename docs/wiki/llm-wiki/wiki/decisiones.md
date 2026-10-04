# Decisiones

## DECISIÓN

- La wiki global vive en `citas-api/docs/wiki/llm-wiki/`.
- RAW es inmutable y WIKI contiene síntesis durable.
- El frontend se integra directamente con Spring Boot, sin BFF.
- Las automatizaciones n8n se versionan como JSON sin credenciales.

## DECISIÓN

- El esquema local heredado de laboratorio se mantiene como baseline Flyway V1.
  La sesión refresh que consume el backend se incorpora mediante una migración
  aditiva V2, sin eliminar la tabla heredada `refresh_tokens`.
- Los adaptadores JPA reflejan los tipos persistidos del baseline: identificador
  de rol `SMALLINT` y hashes/JTI de refresh en `CHAR`.

Estas decisiones proceden del contexto aprobado y las fuentes técnicas
curadas; las decisiones de diseño de API y datos quedan por aprobar.

## DECISIÓN — Reconciliación S2–S6 (2026-10-02/04)

- **Arquitectura hexagonal verificable:** dominio sin frameworks, aplicación sin
  Spring/JDBC/JPA, persistencia solo en `adapter/out/persistence`; lo impone
  `HexagonalArchitectureTest`.
- **Errores semánticos:** Problem Details con `code` estable; 400/401/403/404/409.
- **Tiempo:** hora local `America/Bogota` con `Clock` inyectable; ISO-8601 sin offset.
- **Reserva:** bloqueo pesimista de los N slots consecutivos de un mismo bloque; solape de
  bloques prohibido entre sedes para el mismo profesional.
- **Cierre "aplicable" (HU-024):** cita propia `APPROVED` con inicio ≤ ahora.
- **Recuperación de contraseña:** respuesta idéntica exista o no la cuenta; token de un solo
  uso con hash; exposición de desarrollo solo por bandera explícita.
- **Automatizaciones:** WF-001 con `X-Automation-Key` de mínimo privilegio y ventana
  deslizante sin duplicados; WF-002 alimentado por outbox transaccional con reintentos, sin
  invalidar la transacción de la cita. `APPOINTMENT_APPROVED` se emite solo por decisión
  ADMIN (no en la reserva general autoaprobada), según el alcance de WF-002 en la guía.
- **Verificación sin Docker:** cuando el equipo no puede ejecutar contenedores, se simula el
  perfil `app` con los mismos artefactos (JAR, entrypoint y `nginx.conf`) y se registra
  explícitamente lo no verificado.
