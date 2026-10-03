# citas-api

Backend Spring Boot 3.5 / Java 21 del laboratorio sintético de agendamiento.
La identidad (registro, login, refresh/logout y recuperación de contraseña)
conserva puertos hexagonales; la vertical de agenda usa transacciones JDBC
detrás de servicios de aplicación para serializar slots (deuda conocida, se
refactoriza en olas posteriores).

## Ejecución

La validación aislada desde base limpia del workspace usa
`../docker-compose.e2e.yml` y `../scripts/verify-e2e.ps1` (raíz del workspace,
fuera de este repo); `../scripts/verify-concurrency.ps1` cubre la carrera de
doble reserva. WF-002 fue importado y probado con evento sintético, respuesta
HTTP 202 y sin credenciales versionadas.

Con MySQL levantado y variables de `.env.example` configuradas:

```powershell
mvn spring-boot:run
```

Flyway crea el esquema 3FN y la oferta demo en una base vacía. Las cuentas de
demo son sintéticas (`*@demo.invalid`) y están documentadas en la migración
`src/main/resources/db/migration/V5__synthetic_demo_seed.sql`; no usarlas fuera
del laboratorio.

La recuperación de contraseña solo devuelve `developmentToken` si
`PASSWORD_RESET_EXPOSE_DEV_TOKEN=true` (propiedad
`app.password-reset.expose-development-token`, por defecto `false`).

Health: `GET /actuator/health`.

## Verificación

Pruebas unitarias y slices web (no requieren base de datos):

```bash
mvn -B test
```

Pruebas de integración REST + persistencia (`*IT.java`, perfil `it`) contra un
MySQL 8.4 real. El datasource llega solo por variables de entorno; la base
indicada **se limpia y se migra con Flyway desde vacío** al iniciar, así que
debe ser una base desechable de pruebas:

```bash
export IT_DB_URL='jdbc:mysql://127.0.0.1:3306/citas_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export IT_DB_USER=<usuario_de_pruebas>
export IT_DB_PASSWORD=<password_de_pruebas>
mvn -B -Pit verify
```

En PowerShell: `$env:IT_DB_URL='...'; $env:IT_DB_USER='...'; $env:IT_DB_PASSWORD='...'; mvn -B -Pit verify`.
Los secretos JWT de las IT son fixtures aleatorios generados en cada ejecución.

El contrato REST canónico vive en `docs/wiki/llm-wiki/wiki/contratos-rest.md`.
Los errores siguen Problem Details (`application/problem+json`) con `code`
estable: `400 VALIDATION_ERROR`/`INVALID_REQUEST`, `401`, `403 FORBIDDEN`,
`404 NOT_FOUND`, `409` para reglas de negocio.

## Documentación compartida
- `docs/wiki/scrum/`: épicas/HU generadas con la Skill Scrum.
- `docs/wiki/llm-wiki/`: única LLM Wiki global del workspace.
- `automations/n8n/`: JSON exportados en S5/S6.
