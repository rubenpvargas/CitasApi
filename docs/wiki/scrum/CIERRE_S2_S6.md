# Cierre S2–S6 — Matriz de evidencia

Fecha de validación: 2026-09-29/30. Datos exclusivamente sintéticos.

## Estado por HU

| HU | Implementación | Evidencia disponible | Estado honesto |
|---|---|---|---|
| 001–003 | Auth, refresh, logout, registro y UI | Maven, E2E limpio, HTTP sintético, Angular lint/test/build | Validación técnica PASS |
| 004 | Token hash, expiración, un solo uso y UI | `verify-edge-flows.ps1` + `verify-password-expiry.ps1` | Validación técnica PASS |
| 005–006 | Perfil y afiliación | `verify-edge-flows.ps1`: PATCH perfil y afiliación normalizada | Validación técnica PASS |
| 007 | Catálogos fijos | HTTP autenticado y migración | Completada |
| 008–015 | EPS, especialidades, profesionales, capacidades, bloques, calendario y disponibilidad | `verify-admin-offer.ps1`, disponibilidad y E2E limpio | Validación técnica PASS |
| 016–018 | Reserva general/especializada y decisión | E2E limpio, smoke `REQUESTED` → `APPROVED`, concurrencia 1 éxito/1 conflicto, auditoría | Validación técnica PASS |
| 019–020 | Mis citas y cancelación | `verify-edge-flows.ps1` y `verify-general-booking.ps1` | Validación técnica PASS |
| 021–022 | Retención y decisión de reprogramación | V4/V6, requests sintéticas 2 y 3 | Validación funcional PASS; falta repetir desde base limpia |
| 023–025 | Agenda, cierre y bandeja | E2E limpio de tres roles, agenda, cierre `COMPLETED`, `403` y bandeja | Validación técnica PASS |

## Validaciones ejecutadas

```text
docker compose config --quiet                         PASS
docker compose --profile dev exec citas-api-dev mvn test -q       PASS
docker compose --profile dev exec citas-web-dev npm run lint       PASS
docker compose --profile dev exec citas-web-dev npm test -- --watch=false --no-progress PASS
docker compose --profile app build citas-api          PASS
docker compose --profile app build citas-web           PASS
GET /actuator/health                                   PASS (UP)
GET /health                                            PASS (Nginx 200)
GET /api/v1/availability?locationCode=HIC             PASS (datos sintéticos)
POST especializada + decisión ADMIN                   PASS (REQUESTED → APPROVED)
POST reprogramación + decisión ADMIN                  PASS (request 2, APPROVED y nueva franja)
POST reprogramación rechazo                            PASS (request 3, original preservada y slot liberado)
E2E desde volumen MySQL nuevo                          PASS (roles USER/ADMIN/PROFESSIONAL)
Concurrencia automatizada                              PASS (1 aceptación + 1 conflicto)
Oferta administrativa y bloques                       PASS (`verify-admin-offer.ps1`)
Reserva Medicina General                               PASS (`verify-general-booking.ps1`)
Flujos de identidad/perfil/afiliación/cancelación     PASS (`verify-edge-flows.ps1`)
Importación WF-001/WF-002 en n8n                     PASS
Webhook WF-002 REJECTED en n8n                         PASS (HTTP 202)
Expiración de recuperación                          PASS (HTTP 409)
```

## Condición de release

La plataforma queda funcional y reproducible en Docker Compose. Las 25 HU tienen implementación y evidencia técnica en los runners, E2E, loops y workflows enumerados en este documento. La expiración se fuerza solo dentro de la base sintética para validar la regla sin esperar tiempo real.
