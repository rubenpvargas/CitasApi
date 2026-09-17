package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.RegisterCommand;
import com.fcv.citas.application.port.in.AuthenticationUseCase;
import com.fcv.citas.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
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

    public AuthController(RegisterUserUseCase registration, AuthenticationUseCase authentication) {
        this.registration = registration;
        this.authentication = authentication;
    }

    @PostMapping("/register")
    ResponseEntity<RegisteredUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var command = new RegisterCommand(request.firstName(), request.lastName(), request.documentType(),
                request.documentNumber(), request.email(), request.phone(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisteredUserResponse.from(registration.register(command)));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authentication.login(request.email(), request.password()));
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
}
