package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.exception.DuplicateIdentifierException;
import com.fcv.citas.application.exception.InvalidCredentialsException;
import com.fcv.citas.application.exception.InvalidRefreshTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DuplicateIdentifierException.class)
    ResponseEntity<ProblemDetail> duplicateIdentifier() {
        return problem(HttpStatus.CONFLICT, "IDENTIFIER_ALREADY_REGISTERED",
                "An account with the supplied identifier already exists");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> invalidCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ProblemDetail> invalidRefreshToken() {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Invalid refresh token");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields are invalid");
        detail.setTitle("Validation failed");
        detail.setProperty("code", "VALIDATION_ERROR");
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
                .toList();
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(detail);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(message);
        detail.setProperty("code", code);
        return ResponseEntity.status(status).body(detail);
    }
}
