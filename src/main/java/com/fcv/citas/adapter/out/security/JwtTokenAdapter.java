package com.fcv.citas.adapter.out.security;

import com.fcv.citas.application.exception.InvalidRefreshTokenException;
import com.fcv.citas.application.model.RefreshTokenClaims;
import com.fcv.citas.application.model.TokenPair;
import com.fcv.citas.application.port.out.TokenPort;
import com.fcv.citas.config.SecurityProperties;
import com.fcv.citas.domain.model.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class JwtTokenAdapter implements TokenPort {
    private static final String TOKEN_TYPE = "token_type";
    private final SecurityProperties properties;
    private final JwtEncoder accessEncoder;
    private final JwtEncoder refreshEncoder;
    private final JwtDecoder refreshDecoder;
    private final JwtDecoder accessDecoder;

    public JwtTokenAdapter(SecurityProperties properties) {
        this.properties = properties;
        SecretKey accessKey = key(properties.accessSecret());
        SecretKey refreshKey = key(properties.refreshSecret());
        this.accessEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(accessKey));
        this.refreshEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(refreshKey));
        this.accessDecoder = decoder(accessKey, properties.issuer(), "access");
        this.refreshDecoder = decoder(refreshKey, properties.issuer(), "refresh");
    }

    @Override
    public TokenPair issuePair(User user, Instant issuedAt) {
        Instant accessExpiresAt = issuedAt.plus(Duration.ofMinutes(properties.accessMinutes()));
        Instant refreshExpiresAt = issuedAt.plus(Duration.ofDays(properties.refreshDays()));
        String refreshJti = UUID.randomUUID().toString();

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(properties.issuer()).issuedAt(issuedAt).expiresAt(accessExpiresAt)
                .subject(user.id().toString()).claim(TOKEN_TYPE, "access")
                .claim("email", user.email()).claim("roles", new ArrayList<>(user.roles())).build();
        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(properties.issuer()).issuedAt(issuedAt).expiresAt(refreshExpiresAt)
                .subject(user.id().toString()).id(refreshJti).claim(TOKEN_TYPE, "refresh").build();

        String accessToken = accessEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), accessClaims)).getTokenValue();
        String refreshToken = refreshEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), refreshClaims)).getTokenValue();
        return new TokenPair(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt, refreshJti);
    }

    @Override
    public RefreshTokenClaims parseRefresh(String token) {
        try {
            Jwt jwt = refreshDecoder.decode(token);
            return new RefreshTokenClaims(Long.valueOf(jwt.getSubject()), jwt.getId(), jwt.getExpiresAt());
        } catch (JwtException | NumberFormatException exception) {
            throw new InvalidRefreshTokenException();
        }
    }

    public JwtDecoder accessDecoder() {
        return accessDecoder;
    }

    private static SecretKey key(String value) {
        return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtDecoder decoder(SecretKey key, String issuer, String expectedType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withType = jwt -> expectedType.equals(jwt.getClaimAsString(TOKEN_TYPE))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong token type", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withType));
        return decoder;
    }
}
