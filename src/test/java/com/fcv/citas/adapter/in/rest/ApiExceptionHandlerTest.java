package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.exception.BusinessException;
import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.ForbiddenException;
import com.fcv.citas.application.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el mapeo transversal de errores a Problem Details con {@code code} estable y estado semántico.
 */
class ApiExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void malformedJsonBodyIs400InvalidRequest() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void beanValidationFailureIs400ValidationErrorWithFieldList() throws Exception {
        mvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void malformedDateTimeInBodyIs400InvalidRequest() throws Exception {
        mvc.perform(post("/probe/datetime").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"2026-13-99\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void malformedDateQueryParameterIs400InvalidRequest() throws Exception {
        mvc.perform(get("/probe/date").param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void missingRequiredParameterIs400InvalidRequest() throws Exception {
        mvc.perform(get("/probe/date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void notFoundExceptionIs404() throws Exception {
        mvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void forbiddenExceptionIs403() throws Exception {
        mvc.perform(get("/probe/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void methodSecurityDenialIs403NotServerError() throws Exception {
        mvc.perform(get("/probe/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void businessRuleExceptionIs409WithItsCode() throws Exception {
        mvc.perform(get("/probe/rule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
    }

    @Test
    void legacyBusinessExceptionKeepsSemanticStatusByCode() throws Exception {
        mvc.perform(get("/probe/legacy").param("code", "NOT_FOUND")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/probe/legacy").param("code", "FORBIDDEN")).andExpect(status().isForbidden());
        mvc.perform(get("/probe/legacy").param("code", "PAST_BLOCK")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAST_BLOCK"));
    }

    @Test
    void unexpectedErrorIs500WithoutInternalDetails() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("at com.fcv"))));
    }

    @RestController
    static class ProbeController {
        record Body(@NotBlank String name) {}

        @PostMapping("/probe/body")
        String body(@Valid @RequestBody Body body) { return body.name(); }

        @PostMapping("/probe/datetime")
        String dateTime(@RequestBody Body body) { return LocalDateTime.parse(body.name()).toString(); }

        @GetMapping("/probe/date")
        String date(@RequestParam LocalDate from) { return from.toString(); }

        @GetMapping("/probe/not-found")
        void notFound() { throw new NotFoundException("Resource not found"); }

        @GetMapping("/probe/forbidden")
        void forbidden() { throw new ForbiddenException("Not your resource"); }

        @GetMapping("/probe/access-denied")
        void accessDenied() { throw new AccessDeniedException("denied"); }

        @GetMapping("/probe/rule")
        void rule() { throw new BusinessRuleException("SLOT_UNAVAILABLE", "Slot taken"); }

        @GetMapping("/probe/legacy")
        void legacy(@RequestParam String code) { throw new BusinessException(code, "legacy"); }

        @GetMapping("/probe/boom")
        void boom() { throw new IllegalStateException("secret-internal-detail"); }
    }
}
