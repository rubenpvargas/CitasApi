# Log de la wiki

| Fecha | Operación | Fuentes/páginas | Resultado |
|---|---|---|---|
| 2026-09-17 | INGEST + INIT | SRC-PRD-001, SRC-TECH-001, SRC-DB-001, SRC-EVID-001, SRC-NOTES-001, READMEs; índice y páginas iniciales | estructura creada; contrato REST queda pendiente |
| 2026-09-17 | DECISIÓN | HU-001, HU-002, HU-003; contratos-rest.md; index.md | contrato de autenticación v1 aprobado e inicio de desarrollo backend registrado |
| 2026-09-22 | DECISIÓN + VALIDACIÓN | decisiones.md; V1/V2 Flyway; arranque local | baseline heredado preservado, V2 de refresh aplicada y API de auth iniciada |
| 2026-09-24 | PLAN CROSS-REPO | contratos-rest.md | login incorporará identidad visible sin exponer credenciales |
| 2026-09-24 | VALIDACIÓN CROSS-REPO | contratos-rest.md; Maven; Angular; navegador local | respuesta `user` consumida por UI y nombre autenticado visible |
| 2026-09-24 | PLAN CROSS-REPO | HU-007; contratos-rest.md | se acuerda consulta autenticada y de solo lectura de catálogos fijos; `citas-web` no tiene consumidor en este incremento |
| 2026-09-24 | VALIDACIÓN | HU-007; V3 Flyway; Maven; HTTP local | catálogos fijos sembrados y consultables con JWT; sin ruta de escritura |
| 2026-09-29 | IMPLEMENTACIÓN + VALIDACIÓN | V4–V7; SchedulingController; SchedulingService; Dockerfiles; compose; CIERRE_S2_S6 | dominio de agenda/reserva/reprogramación, healthchecks, SPA de producción y matriz de evidencia añadidos; pruebas ejecutadas con datos sintéticos |
| 2026-09-30 | TRAZABILIDAD | LOOP_01, LOOP_02, LOOP_03; WF-001/WF-002 JSON | loops versionados y workflows exportables sin credenciales; concurrencia, E2E de tres roles y ejecución n8n quedan explícitamente pendientes |
| 2026-09-30 | VALIDACIÓN + CORRECCIÓN | V2 Flyway; docker-compose.e2e.yml; verify-e2e.ps1; verify-concurrency.ps1; n8n temporal | V2 quedó idempotente para base limpia; E2E limpio, carrera de reserva, importación de ambos workflows y webhook WF-002 HTTP 202 verificados |
| 2026-09-30 | VALIDACIÓN CROSS-REPO | verify-edge-flows.ps1; verify-admin-offer.ps1; verify-general-booking.ps1; contrato `/admin/locations` | perfil, afiliación, CRUD de oferta, capacidades, bloques, reserva general, cancelación y sede normalizada verificados con datos sintéticos |
| 2026-10-02 | AUDITORÍA + VALIDACIÓN | HU-001–004, HU-007; contratos-rest.md (planes Olas A–F, decisión de zona horaria); IT MySQL; smoke HTTP | HECHO: auditoría halló HU marcadas Completada sin evidencia, UI con mocks y sin pruebas fuera de identidad; Ola A corregida (no enumeración, token de un solo uso, errores semánticos, guards/refresh en UI) y cerrada con 42 unitarias + 38 IT + 91 specs web. DECISIÓN: hora local America/Bogota con Clock inyectable. PREGUNTA ABIERTA: virtualización deshabilitada impide Docker en el equipo del estudiante |
| 2026-10-03 | PAUSA | Olas B/C | HECHO: Ola B implementada en API (62 unitarias + 55 IT) y web; HU-010–014 implementadas en web; pendiente HU-010–015 en API, HU-015 en web y verificación/evidencia de Olas B–C |
| 2026-10-03 | VALIDACIÓN + PAUSA | Olas B y C cerradas; D/E parcial; n8n; hooks | HECHO: HU-005–015 cerradas con IT, pruebas de escritorio y E2E Playwright UI+API real (6/9; 04–06 dependen de D/E); HU-016–021 implementadas en API (140 unitarias + 94 IT verificadas) y HU-016–025 en web (268 specs); hooks pre-commit/pre-push; WF-001/WF-002 con credenciales de mínimo privilegio y validador. PENDIENTE: HU-022–025 API, Ola G backend, loops S4, cierre de evidencia D–F, release |
