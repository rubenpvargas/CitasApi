package com.fcv.citas.domain.model;

/** Entidad promotora de salud sintética; retirar es desactivar (sin borrado físico). */
public record Eps(Long id, String code, String name, boolean active) {
}
