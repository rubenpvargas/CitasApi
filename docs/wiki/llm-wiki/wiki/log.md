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
