# Riesgos y preguntas abiertas

## RIESGO

- La inspección inicial encontró un `.git` adicional en la raíz, incompatible
  con la regla de dos repositorios; no se ha alterado.
- Ninguno de los repositorios tiene actualmente la rama `develop`.
- Hay cambios preexistentes en `.env.example`; deben preservarse y no contienen
  una decisión atribuida por esta wiki.

## PREGUNTA ABIERTA

- ¿Se elimina o conserva el `.git` raíz?
- ¿Cómo se crean las ramas `develop` sin reescribir historial?
- ¿Cuál es el contrato REST canónico y su política temporal/error?
- ¿Qué expiración tienen las retenciones pendientes?
- ¿Cómo se inicializa el primer `ADMIN` y cuál es la política concreta de JWT?
- ¿Puede un usuario tener varias afiliaciones activas y cómo se trata
  `Medicina General` en el catálogo?

## Actualización 2026-10-04

HECHO (resueltas): ya no existe `.git` raíz; `develop` existe en ambos repos creada desde
`origin/develop` sin reescribir historial; el contrato canónico y la política temporal y de
errores están en [contratos-rest.md](contratos-rest.md); el primer ADMIN es sintético y se
siembra en V5; cada usuario tiene una afiliación vigente (`is_current`) con historial
reactivable; existe a lo sumo una especialidad general activa.

PREGUNTA ABIERTA:
- Las retenciones (`REQUESTED` y reprogramación `PENDING`) no expiran: permanecen hasta la
  decisión ADMIN o la cancelación. ¿Se define un vencimiento automático?
- Docker Compose no se ha ejecutado en el equipo del estudiante (plataforma de máquina
  virtual deshabilitada); la evidencia de despliegue es una simulación.
- Un paciente puede reservar citas simultáneas con profesionales distintos; el PRD no lo
  prohíbe.
