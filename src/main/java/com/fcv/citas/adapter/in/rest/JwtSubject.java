package com.fcv.citas.adapter.in.rest;

import org.springframework.security.oauth2.jwt.Jwt;

/** Deriva el actor autenticado del sub del access JWT (nunca del cuerpo ni de la ruta). */
final class JwtSubject {
    private JwtSubject() {
    }

    static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
