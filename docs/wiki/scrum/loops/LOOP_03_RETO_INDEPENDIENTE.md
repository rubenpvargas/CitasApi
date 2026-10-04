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

---

## Ejecución 2 — reto: reconciliar contrato frontend/backend hasta E2E verde (2026-10-03/04)

1. **Disparador.** Frontend y backend se construyeron en paralelo contra
   `contratos-rest.md`; las specs del frontend usan HTTP simulado, así que una divergencia de
   nombres, códigos o forma de respuesta solo aparece al integrar.
2. **Meta verificable.** Los 9 escenarios Playwright (`CitasWeb/e2e`, tres roles) en verde
   contra la API real y MySQL desde base vacía, sin relajar aserciones.
3. **Estado observado/persistente.** Resultado de Playwright, `contratos-rest.md` y suites
   `mvn -Pit verify` / `ng test` versionadas.
4. **Alcance del Builder.** Agente backend o frontend según el lado divergente, solo para
   ajustar al contrato; un cambio de contrato requiere entrada nueva en `contratos-rest.md`.
5. **Evidencia del Verifier.** Ejecución Playwright, verificación independiente en worktree y
   revisión de que no se modificaron aserciones del E2E.
6. **Presupuesto.** 3 iteraciones.
7. **Parada.** 9/9 PASS.
8. **Escalamiento humano.** Si una divergencia exige cambiar una regla del PRD o tras 3
   iteraciones.

| Iteración | Fecha | Entorno | Resultado | Decisión | Feedback |
|---|---|---|---|---|---|
| 1 | 2026-10-03 | API `028ecf8` (Olas A–C) + `ng serve` | 6/9; fallan 04 (sin `cancellable`), 05 (sin `rejectionReason`), 06 (sin `reschedulable`) | **FAIL** | Las divergencias estaban en el backend pendiente (Olas D/E) y coincidían con el contrato ya escrito; se precisaron campos de bandeja (`9ed268d`). Sin cambios al E2E. |
| 2 | 2026-10-04 | Stack simulado de `--profile app`: MySQL nuevo, API `fd19712` con el entorno del compose, SPA de producción en nginx 1.27 con la configuración del repo (puerto 4200), simulador WF-002 | **9/9 PASS** (34 s) | **PASS** | — |

10. **Por qué no bastaba un prompt.** La divergencia solo es observable ejecutando ambos lados
    juntos; cada iteración depende del resultado real de la anterior y de trabajo de dos
    agentes en repos distintos.

**Parada.** PASS en la iteración 2 de 3, sin modificar aserciones del E2E ni el contrato
fuera de la precisión aditiva registrada.
