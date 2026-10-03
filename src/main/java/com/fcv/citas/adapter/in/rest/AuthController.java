package com.fcv.citas.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fcv.citas.application.model.RegisterCommand;
import com.fcv.citas.application.port.in.AuthenticationUseCase;
import com.fcv.citas.application.port.in.PasswordRecoveryUseCase;
import com.fcv.citas.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    static final String PASSWORD_RESET_ACCEPTED_MESSAGE =
            "If the account exists, password recovery instructions were generated";

    private final RegisterUserUseCase registration;
    private final AuthenticationUseCase authentication;
    private final PasswordRecoveryUseCase recovery;

    public AuthController(RegisterUserUseCase registration, AuthenticationUseCase authentication,
                          PasswordRecoveryUseCase recovery) {
        this.registration = registration;
        this.authentication = authentication;
        this.recovery = recovery;
    }

    @PostMapping("/register")
    ResponseEntity<RegisteredUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var command = new RegisterCommand(request.firstName(), request.lastName(), request.documentType(),
                request.documentNumber(), request.email(), request.phone(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisteredUserResponse.from(registration.register(command)));
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(authentication.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(authentication.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authentication.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** 202 con cuerpo idéntico exista o no la cuenta; {@code developmentToken} solo con la bandera de desarrollo. */
    @PostMapping("/password-reset/request")
    ResponseEntity<PasswordResetResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        var result = recovery.requestReset(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new PasswordResetResponse(
                PASSWORD_RESET_ACCEPTED_MESSAGE, result.developmentToken().orElse(null)));
    }

    @PostMapping("/password-reset/confirm")
    ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        recovery.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    record PasswordResetRequest(@NotBlank @Email @Size(max = 254) String email) {}

    record PasswordResetConfirmRequest(@NotBlank @Size(max = 128) String token,
                                       @NotBlank @StrongPassword String newPassword) {
        @Override
        public String toString() {
            return "PasswordResetConfirmRequest[token=***, newPassword=***]";
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PasswordResetResponse(String message, String developmentToken) {}
}
