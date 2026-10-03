package com.fcv.citas.adapter.out.security;

import com.fcv.citas.application.port.out.ResetTokenGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** 256 bits de entropía codificados en Base64 URL-safe (aptos para {@code ?token=} sin escape). */
@Component
public class SecureRandomResetTokenGenerator implements ResetTokenGeneratorPort {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
