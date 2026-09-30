# citas-api

Backend Spring Boot 3.5 / Java 21 del laboratorio sintético de agendamiento.
La identidad conserva puertos hexagonales, y la vertical de agenda usa
transacciones JDBC detrás de servicios de aplicación para serializar slots.

## Ejecución

La validación aislada desde base limpia usa `docker-compose.e2e.yml` y
`scripts/verify-e2e.ps1`; `scripts/verify-concurrency.ps1` cubre la carrera de
doble reserva. WF-002 fue importado y probado con evento sintético, respuesta
HTTP 202 y sin credenciales versionadas.

Con MySQL levantado y variables de `.env.example` configuradas:

```powershell
mvn spring-boot:run
```

Flyway crea el esquema 3FN y la oferta demo en una base vacía. La contraseña
de demo sintética es `Demo1234*`; no usarla fuera del laboratorio.

Health: `GET /actuator/health`.

## Verificación

```powershell
mvn test
```

El contrato REST canónico vive en `docs/wiki/llm-wiki/wiki/contratos-rest.md`.

## Documentación compartida
- `docs/wiki/scrum/`: épicas/HU generadas con la Skill Scrum.
- `docs/wiki/llm-wiki/`: única LLM Wiki global del workspace.
- `automations/n8n/`: JSON exportados en S5/S6.

Lee el PRD en la carpeta raíz antes de inicializar Spring Boot.
