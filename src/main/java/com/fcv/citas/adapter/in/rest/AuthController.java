package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.RegisterCommand;
import com.fcv.citas.application.port.in.AuthenticationUseCase;
import com.fcv.citas.application.port.in.RegisterUserUseCase;
import com.fcv.citas.application.service.PasswordRecoveryService;
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
    private final RegisterUserUseCase registration;
    private final AuthenticationUseCase authentication;
    private final PasswordRecoveryService recovery;

    public AuthController(RegisterUserUseCase registration, AuthenticationUseCase authentication,
                          PasswordRecoveryService recovery) {
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

    @PostMapping("/password-reset/request")
    PasswordResetResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return new PasswordResetResponse("If the account exists, a recovery token was generated", recovery.request(request.email()));
    }

    @PostMapping("/password-reset/confirm")
    ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        recovery.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    record PasswordResetRequest(@Email @NotBlank String email) {}
    record PasswordResetConfirmRequest(@NotBlank String token, @NotBlank @Size(min=8, max=72) String newPassword) {}
    record PasswordResetResponse(String message, String developmentToken) {}
}
