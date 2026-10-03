package com.fcv.citas.adapter.in.rest;

import com.fcv.citas.application.model.ProfileUpdateCommand;
import com.fcv.citas.application.port.in.ProfileUseCase;
import com.fcv.citas.domain.model.UserProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-005 — el titular es siempre el sub del access JWT; no hay rutas por id de usuario. */
@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {
    private final ProfileUseCase profiles;

    public ProfileController(ProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return ProfileResponse.from(profiles.getProfile(JwtSubject.userId(jwt)));
    }

    @PatchMapping
    ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileUpdateRequest request) {
        return ProfileResponse.from(profiles.updateProfile(JwtSubject.userId(jwt),
                new ProfileUpdateCommand(request.firstName(), request.lastName(), request.phone())));
    }

    /** Campos desconocidos (email, documento, id) se ignoran: no son editables en v1. */
    record ProfileUpdateRequest(@NotBlank @Size(max = 100) String firstName,
                                @NotBlank @Size(max = 100) String lastName,
                                @NotBlank @Size(max = 32) String phone) {
    }

    record ProfileResponse(Long id, String firstName, String lastName, String email, String documentType,
                           String documentNumber, String phone, List<String> roles) {
        static ProfileResponse from(UserProfile p) {
            return new ProfileResponse(p.id(), p.firstName(), p.lastName(), p.email(), p.documentType(),
                    p.documentNumber(), p.phone(), p.roles().stream().sorted().toList());
        }
    }
}
