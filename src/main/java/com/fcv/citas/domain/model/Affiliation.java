package com.fcv.citas.domain.model;

/** Afiliación del usuario a un plan (y por él a una EPS y un régimen) mediante referencias normalizadas. */
public record Affiliation(Long id, String membershipNumber, boolean current, EpsPlan plan) {
}
