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
