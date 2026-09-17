# Extracto curado — requisitos de normalización

Fuente: `database/REQUISITOS_NORMALIZACION_3FN.md`. Estado: aprobado para
ingestión; el modelo de referencia del trainer queda diferido.

- Representar usuarios/roles, profesionales, especialidades y sedes N:M,
  afiliación, disponibilidad, citas, estados, auditoría y tokens.
- 1FN: atributos atómicos y sin listas en columnas.
- 2FN: las relaciones N:M usan tablas puente y los atributos dependen de toda
  la clave compuesta.
- 3FN: no repetir nombres de catálogos ni introducir dependencias transitivas.
- El diseño debe justificar reservas, citas de 60 minutos, reprogramación sin
  perder la cita original, snapshots/FK e índices de agenda.
