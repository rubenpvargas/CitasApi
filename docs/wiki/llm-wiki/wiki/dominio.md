# Dominio

## HECHO

El sistema académico usa actores `USER`, `PROFESSIONAL` y `ADMIN`, dos sedes
fijas, catálogos de especialidad/EPS/planes, disponibilidad en slots de 30
minutos, citas generales y especializadas, reprogramación, cancelación,
auditoría y cierre (`COMPLETED`/`NO_SHOW`).

## Invariantes

No se reservan slots ocupados, una cita de 60 minutos requiere dos slots
consecutivos, no se agenda en el pasado, y las transiciones de estado son
explícitas y auditables.

Fuente: [PRD curado](../raw/prd-v1.0.md).
