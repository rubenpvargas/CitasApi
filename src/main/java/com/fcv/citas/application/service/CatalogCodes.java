package com.fcv.citas.application.service;

import java.util.Locale;

/** Normalización de códigos técnicos de catálogo: sin espacios y en mayúsculas. */
final class CatalogCodes {
    private CatalogCodes() {
    }

    static String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
