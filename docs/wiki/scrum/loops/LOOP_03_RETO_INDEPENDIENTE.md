# LOOP_03 — Regresión de producto y mínimo privilegio

## Objetivo

Comprobar que API, SPA, MySQL y Docker funcionan juntos y que las pantallas de operación respetan roles.

## Resultado

- API Docker construida y pruebas Maven ejecutadas con resultado PASS.
- SPA lint, test unitario y build Docker ejecutados con resultado PASS.
- Health API y `/health` de Nginx comprobados con datos sintéticos.
- Rama `develop` creada en `citas-web`; `citas-api` ya estaba en `develop`.
- Prueba completa de tres roles y carga limpia desde volumen nuevo: PASS con `scripts/verify-e2e.ps1`.

## Decisión

PASS de regresión técnica; E2E de tres roles y validación controlada de workflows n8n completadas. Las pruebas específicas restantes están enumeradas en la matriz de cierre.
