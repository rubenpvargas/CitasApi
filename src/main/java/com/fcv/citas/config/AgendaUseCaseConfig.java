package com.fcv.citas.config;

import com.fcv.citas.application.port.in.ProfileUseCase;
import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.service.ProfileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Casos de uso de perfil, catálogos administrables, profesionales y agenda (olas B y C). */
@Configuration
public class AgendaUseCaseConfig {
    @Bean
    ProfileUseCase profileUseCase(ProfileRepositoryPort profiles, TransactionPort transactions, Clock clock) {
        return new ProfileService(profiles, transactions, clock);
    }
}
