package com.fcv.citas.adapter.in.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Ola G — credencial de automatización: sin clave configurada → 401; clave correcta → solo ROLE_AUTOMATION. */
class AutomationKeyAuthenticationFilterTest {
    private static final String KEY = "clave-sintetica-de-prueba";

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unsetConfiguredKeyRejectsAnyPresentedKey() throws Exception {
        MockHttpServletResponse response = run(null, KEY, new AtomicReference<>());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void wrongKeyIs401AndChainIsNotInvoked() throws Exception {
        AtomicReference<String> seenRole = new AtomicReference<>();
        MockHttpServletResponse response = run(KEY, KEY + "x", seenRole);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(seenRole.get()).isNull();
    }

    @Test
    void validKeyAuthenticatesWithAutomationRoleOnlyForTheRequest() throws Exception {
        AtomicReference<String> seenRole = new AtomicReference<>();
        MockHttpServletResponse response = run(KEY, KEY, seenRole);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(seenRole.get()).isEqualTo("ROLE_AUTOMATION");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void absentHeaderPassesThroughUntouched() throws Exception {
        AtomicReference<String> seenRole = new AtomicReference<>("untouched");
        MockHttpServletResponse response = run(KEY, null, seenRole);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(seenRole.get()).isEqualTo("none");
    }

    private static MockHttpServletResponse run(String configured, String presented, AtomicReference<String> seenRole)
            throws Exception {
        var filter = new AutomationKeyAuthenticationFilter(configured, r -> r.setStatus(401));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/automation/appointments/reminders");
        if (presented != null) {
            request.addHeader(AutomationKeyAuthenticationFilter.HEADER, presented);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                seenRole.set(auth == null ? "none" : auth.getAuthorities().iterator().next().getAuthority());
            }
        });
        return response;
    }
}
