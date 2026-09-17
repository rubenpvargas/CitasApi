# Arquitectura

## HECHO

`citas-api` será Java 21/Spring Boot 3.5.x con arquitectura hexagonal, JPA,
MySQL 8.4, Flyway, Spring Security/JWT y REST JSON. `citas-web` será React o
Angular con TypeScript y consumirá la API directamente; no hay Express/BFF.

La lógica de negocio permanece en API, la UI en web y los workflows n8n se
versionan como JSON bajo `citas-api/automations/n8n/`.

Fuente: [restricciones](../raw/restricciones-tecnicas.md).
