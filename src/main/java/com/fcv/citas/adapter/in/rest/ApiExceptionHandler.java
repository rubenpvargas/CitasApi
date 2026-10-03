package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.exception.BusinessException;
import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.DuplicateIdentifierException;
import com.fcv.citas.application.exception.ForbiddenException;
import com.fcv.citas.application.exception.InvalidCredentialsException;
import com.fcv.citas.application.exception.InvalidPasswordException;
import com.fcv.citas.application.exception.InvalidRefreshTokenException;
import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.domain.model.DomainRuleViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Traduce excepciones a Problem Details (RFC 9457) con propiedad {@code code} estable y estado semántico:
 * 400 entrada inválida, 401 autenticación, 403 rol/ownership, 404 recurso, 409 regla de negocio.
 * Nunca devuelve trazas, mensajes internos ni tokens.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Object> notFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found");
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    ResponseEntity<Object> forbidden() {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> unauthenticated() {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
    }

    @ExceptionHandler(DomainRuleViolation.class)
    ResponseEntity<Object> domainRule(DomainRuleViolation exception) {
        return problem(HttpStatus.CONFLICT, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<Object> businessRule(BusinessRuleException exception) {
        return problem(HttpStatus.CONFLICT, exception.code(), exception.getMessage());
    }

    /** Excepción heredada de la vertical de agenda; se refactorizará en olas posteriores. */
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> business(BusinessException exception) {
        return switch (exception.code()) {
            case "NOT_FOUND" -> problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found");
            case "FORBIDDEN" -> forbidden();
            default -> problem(HttpStatus.CONFLICT, exception.code(), exception.getMessage());
        };
    }

    @ExceptionHandler(RequestValidationException.class)
    ResponseEntity<Object> requestValidation(RequestValidationException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields are invalid");
        detail.setTitle("Validation failed");
        detail.setProperty("code", "VALIDATION_ERROR");
        detail.setProperty("errors", List.of(Map.of("field", exception.field(), "message", exception.getMessage())));
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(detail);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    ResponseEntity<Object> invalidPassword(InvalidPasswordException exception) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(DateTimeParseException.class)
    ResponseEntity<Object> malformedDate() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "A date or time value is malformed");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> integrity() {
        return problem(HttpStatus.CONFLICT, "INTEGRITY_ERROR", "The operation conflicts with existing data");
    }

    @ExceptionHandler(DuplicateIdentifierException.class)
    ResponseEntity<Object> duplicateIdentifier() {
        return problem(HttpStatus.CONFLICT, "IDENTIFIER_ALREADY_REGISTERED",
                "An account with the supplied identifier already exists");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Object> invalidCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<Object> invalidRefreshToken() {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception exception) {
        // Solo el tipo: los mensajes pueden contener datos de entrada (p. ej. tokens o SQL).
        LOG.error("Unexpected error handling request: {}", exception.getClass().getName());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields are invalid");
        detail.setTitle("Validation failed");
        detail.setProperty("code", "VALIDATION_ERROR");
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(),
                        "message", Objects.toString(error.getDefaultMessage(), "invalid value")))
                .toList();
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(detail);
    }

    /** Completa los Problem Details generados por Spring MVC con un {@code code} estable por estado. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, @Nullable Object body,
                                                             HttpHeaders headers, HttpStatusCode status,
                                                             WebRequest request) {
        ProblemDetail detail = body instanceof ProblemDetail existing
                ? existing : ProblemDetail.forStatus(status);
        if (detail.getProperties() == null || !detail.getProperties().containsKey("code")) {
            detail.setProperty("code", codeFor(status));
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.addAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(detail, responseHeaders, status);
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "INVALID_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> status.is4xxClientError() ? "INVALID_REQUEST" : "INTERNAL_ERROR";
        };
    }

    private static ResponseEntity<Object> problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(message);
        detail.setProperty("code", code);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(detail);
    }
}
