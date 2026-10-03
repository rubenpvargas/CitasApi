package com.fcv.citas.config;

import com.fcv.citas.application.port.in.AuthenticationUseCase;
import com.fcv.citas.application.port.in.CatalogQueryUseCase;
import com.fcv.citas.application.port.in.PasswordRecoveryUseCase;
import com.fcv.citas.application.port.in.RegisterUserUseCase;
import com.fcv.citas.application.port.out.CatalogRepositoryPort;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.PasswordResetTokenRepositoryPort;
import com.fcv.citas.application.port.out.RefreshSessionRepositoryPort;
import com.fcv.citas.application.port.out.RefreshSessionRevocationPort;
import com.fcv.citas.application.port.out.ResetTokenGeneratorPort;
import com.fcv.citas.application.port.out.TokenHashPort;
import com.fcv.citas.application.port.out.TokenPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.port.out.UserCredentialPort;
import com.fcv.citas.application.port.out.UserRepositoryPort;
import com.fcv.citas.application.service.AuthenticationService;
import com.fcv.citas.application.service.CatalogQueryService;
import com.fcv.citas.application.service.PasswordRecoveryService;
import com.fcv.citas.application.service.RegisterUserService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(PasswordResetProperties.class)
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepositoryPort users, PasswordHashPort passwords,
                                            TransactionPort transactions) {
        return new RegisterUserService(users, passwords, transactions);
    }

    @Bean
    CatalogQueryUseCase catalogQueryUseCase(CatalogRepositoryPort catalogs) {
        return new CatalogQueryService(catalogs);
    }

    @Bean
    AuthenticationUseCase authenticationUseCase(UserRepositoryPort users,
                                                 RefreshSessionRepositoryPort sessions,
                                                 PasswordHashPort passwords, TokenPort tokens,
                                                 TokenHashPort tokenHashes, TransactionPort transactions,
                                                 Clock clock) {
        return new AuthenticationService(users, sessions, passwords, tokens, tokenHashes,
                transactions, clock);
    }

    @Bean
    PasswordRecoveryUseCase passwordRecoveryUseCase(UserRepositoryPort users,
                                                    PasswordResetTokenRepositoryPort resetTokens,
                                                    UserCredentialPort credentials,
                                                    RefreshSessionRevocationPort sessions,
                                                    PasswordHashPort passwords, TokenHashPort tokenHashes,
                                                    ResetTokenGeneratorPort generator,
                                                    TransactionPort transactions, Clock clock,
                                                    PasswordResetProperties properties) {
        return new PasswordRecoveryService(users, resetTokens, credentials, sessions, passwords, tokenHashes,
                generator, transactions, clock, Duration.ofMinutes(properties.tokenMinutes()),
                properties.exposeDevelopmentToken());
    }
}
