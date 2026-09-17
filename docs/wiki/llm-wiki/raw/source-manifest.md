# Manifiesto de fuentes RAW

Estado: inicial, curado e inmutable. Las fuentes canónicas permanecen en el
workspace raíz; este manifiesto identifica qué se ingestó y evita duplicar
conversaciones o secretos.

| ID | Fuente canónica | Uso |
|---|---|---|
| SRC-PRD-001 | `PRD.md`, versión 1.0 | alcance, actores y reglas funcionales |
| SRC-TECH-001 | `RESTRICCIONES_TECNICAS.md` | arquitectura, stack y límites |
| SRC-DB-001 | `database/REQUISITOS_NORMALIZACION_3FN.md` | modelo y criterios 1FN–3FN |
| SRC-EVID-001 | `EVIDENCIAS_Y_TRAZABILIDAD.md` | evidencia por sesión y Git |
| SRC-NOTES-001 | `FUENTES_Y_NOTAS.md` | procedencia pública y patrón wiki |
| SRC-API-README-001 | `citas-api/README.md` | estado inicial del backend |
| SRC-WEB-README-001 | `citas-web/README.md` | estado inicial y elección de framework |

Los snapshots de implementación futura (HU/DoD, ADR, contrato REST y
workflows n8n) se añadirán con un nuevo ID, versión y fecha; nunca se
reescribirá una entrada existente.
