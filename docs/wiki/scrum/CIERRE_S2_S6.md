# Cierre S2–S6 — Matriz de evidencia

Revisión: 2026-10-04. Datos exclusivamente sintéticos. Rama `develop` en ambos repos.

> La versión del 2026-09-29/30 declaraba las 25 HU validadas, pero las HU tenían DoD sin
> marcar, la UI usaba datos simulados y solo había 11 pruebas automatizadas. La auditoría del
> 2026-10-02 la reemplazó por esta matriz; la versión anterior se conserva en el historial Git.

## Estado por HU

| HU | API | Web | Evidencia principal | Estado |
|---|---|---|---|---|
| 001–004, 007 | `d424977` `2fdb2a8` `51b50e6` `52b5684` `d2ce597` | `6099f8e` `bd91419` `74ad56a` `44c8701` `0b475f4` | IT registro/login/401-403/refresh/recuperación; smoke HTTP; E2E identidad | Cerrada |
| 005, 006, 008, 009 | `704db7e` `6197955` `6766460` `cf886f0` | `c5ea141` `199ce56` `946615d` `6de9b64` | IT perfil, afiliación, EPS/planes, especialidades; smoke HTTP; E2E catálogos ADMIN | Cerrada |
| 010–015 | `5f020c3` `e57fbf5` `0d2727b` `ba818ef` `227ffcc` `1becd7c` | `61653b7` `3c6e111` `06edca8` `8f6f48f` `41f5a7e` `2a25c53` | IT y pruebas de escritorio de bloques, slots y disponibilidad 30/60; E2E bloque | Cerrada |
| 016–018 | `398268a` `5e70722` | `7455ceb` `3f46577` `123fa63` | IT concurrente (1×201 + 1×409); LOOP_01 PASS; E2E reserva y rechazo | Cerrada |
| 019–022 | `c00ac79` `1588ed5` `581ffa8` `1f0b78b` | `9bfc827` `6e727b0` `63f99b5` `39d1012` | IT de cancelación y ciclo de reprogramación; LOOP_02 PASS; E2E desde base limpia | Cerrada |
| 023–025 | `9ae3120` `b8fb954` `6dc7a47` | `0385e8e` `fdf68b0` `576e351` | IT agenda, cierre y bandeja con filtros; E2E agenda | Cerrada |

Cada HU contiene su tabla de evidencia y su historial en `historias-de-usuario/`. Las trazas
manuales están en [PRUEBAS_DE_ESCRITORIO.md](PRUEBAS_DE_ESCRITORIO.md).

## Validaciones ejecutadas (2026-10-04)

| Verificación | Resultado |
|---|---|
| API `mvn -B -Pit verify` (worktree aislado `fd19712`, MySQL 8.4 vacío) | 178 unitarias + 111 IT, 0 fallos |
| `HexagonalArchitectureTest` (dominio sin frameworks, aplicación sin Spring/JDBC/JPA, sin `Map` en REST) | PASS |
| Web `npm run lint` / `ng test` / `npm run build` (`a1baa5a`) | OK / 268 specs / OK |
| Flyway V1→V10 sobre base vacía | PASS |
| E2E Playwright, 9 escenarios, tres roles, sobre stack simulado | 9/9 PASS |
| WF-002: outbox → simulador n8n con la lógica real del nodo de validación | 3 eventos `SENT`, sin campos sensibles, cabecera `X-Event-Id` |
| WF-002 con n8n caído | cancelación 200; evento `PENDING`/`IO_ERROR` → `SENT` al reintentar |
| WF-001: `X-Automation-Key` y ventana deslizante | 401/401/403/400 en negativos; 1 cita en ventana, 0 en la adyacente |
| `node automations/n8n/validate-workflows.mjs` | 32/32 PASS |
| Reinicio sin borrar datos | datos conservados; Flyway "up to date" |
| Respaldo `mysqldump` y restauración en base nueva | conteos idénticos |
| Hooks: secreto ficticio en commit | bloqueado (sin mostrar el valor) |
| Contenedor web simulado (entrypoint real + nginx 1.27 + `nginx.conf` del repo) | `nginx -t` OK, fallback SPA, `no-store`/`immutable`, CSP con `connect-src` a la API, `/health` 200 |
| `docker compose config` (ambos archivos) | válido |

## Simulación del despliegue sin Docker

El equipo de desarrollo no puede ejecutar Docker: la "Plataforma de máquina virtual" de
Windows está deshabilitada, por lo que WSL2 y Docker Desktop no arrancan (la virtualización
AMD-V del firmware sí está activa). Se simuló `docker compose --profile app up` con los mismos
artefactos y variables:

| Servicio compose | Simulación |
|---|---|
| `mysql` (volumen nuevo) | MySQL 8.4.8 portable, base vacía por ejecución |
| `citas-api` | JAR de `fd19712` con el entorno del compose (`FRONTEND_ORIGIN`, `APP_TIME_ZONE`, `AUTOMATION_API_KEY`, `N8N_*`), secretos aleatorios no registrados |
| `citas-web` | `docker/entrypoint.sh` real + nginx 1.27.5 con `docker/nginx.conf`, puerto 4200 como el mapeo del compose |
| n8n (WF-002) | servidor que exige `X-Webhook-Secret` y ejecuta el código del nodo `Validate event` del JSON exportado |

**No verificado (requiere Docker):** `docker compose build`, imágenes multi-stage, usuario no
root dentro del contenedor, `HEALTHCHECK` de ambas imágenes y resolución de nombres entre
servicios. Comandos para cerrarlo cuando Docker esté disponible:

```powershell
docker compose --profile app up -d --build
docker compose ps                      # api y web healthy
cd CitasWeb; $env:E2E_BASE_URL="http://localhost:4200"; npm run e2e
docker compose restart citas-api       # persistencia
```

## Condición de release

Todas las HU tienen CA, DoD y evidencia; no hay doble reserva; pruebas, hooks y contrato pasan;
loops y workflows están versionados sin secretos. Queda pendiente únicamente la verificación
real con Docker Compose exigida por el criterio de producto terminado; hasta entonces `develop`
se etiqueta como candidato (`v1.0.0-rc1`) y `main` no se actualiza.
