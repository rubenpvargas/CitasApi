package com.fcv.citas.adapter.in.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Autentica la cabecera {@code X-Automation-Key} comparando en tiempo constante el SHA-256 de la clave
 * presentada con el de {@code AUTOMATION_API_KEY}. Concede solo {@code ROLE_AUTOMATION}; la autorización
 * de SecurityConfig la limita a {@code /api/v1/automation/**}. Sin clave configurada, cualquier clave
 * presentada se rechaza con 401. La clave nunca se registra.
 */
public class AutomationKeyAuthenticationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Automation-Key";
    public static final String ROLE = "ROLE_AUTOMATION";

    private final byte[] expectedHash;
    private final ProblemWriter unauthorized;

    public interface ProblemWriter {
        void write(HttpServletResponse response) throws IOException;
    }

    public AutomationKeyAuthenticationFilter(String configuredKey, ProblemWriter unauthorized) {
        this.expectedHash = configuredKey == null || configuredKey.isBlank() ? null : sha256(configuredKey);
        this.unauthorized = unauthorized;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented == null) {
            chain.doFilter(request, response);
            return;
        }
        if (expectedHash == null || !MessageDigest.isEqual(expectedHash, sha256(presented))) {
            SecurityContextHolder.clearContext();
            unauthorized.write(response);
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken("automation", null,
                List.of(new SimpleGrantedAuthority(ROLE)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
