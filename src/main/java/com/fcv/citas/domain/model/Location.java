package com.fcv.citas.domain.model;

/** Sede fija del laboratorio (HIC/ICV) con su ubicación pública. */
public record Location(long id, String code, String name, String address, String city, String department,
                       boolean active) {
}
