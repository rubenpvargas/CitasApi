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
