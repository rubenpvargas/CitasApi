# Extracto curado — restricciones técnicas

Fuente: `RESTRICCIONES_TECNICAS.md`. Estado: aprobado para ingestión.

- API: Java 21, Spring Boot 3.5.x, Maven, hexagonal, JPA, MySQL 8.4,
  Flyway, Spring Security/JWT y REST JSON.
- Web: Node 24, TypeScript, React o Angular según Stitch/AI Studio; REST
  directo y URL configurable por environment.
- Base de datos en 3FN; catálogos fijos por seed y datos sintéticos.
- Solo dos repos públicos: `citas-api` y `citas-web`; `develop` es trabajo y
  `main` estable.
- No versionar passwords, secretos JWT, OAuth, tokens MCP ni credenciales.
- Los JSON de n8n se versionan en `citas-api/automations/n8n/` sin secretos.
