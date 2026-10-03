package com.fcv.citas.config;

import com.fcv.citas.application.port.in.AffiliationUseCase;
import com.fcv.citas.application.port.in.InsuranceCatalogUseCase;
import com.fcv.citas.application.port.in.ProfessionalAdminUseCase;
import com.fcv.citas.application.port.in.ProfileUseCase;
import com.fcv.citas.application.port.in.SpecialtyUseCase;
import com.fcv.citas.application.port.out.AffiliationRepositoryPort;
import com.fcv.citas.application.port.out.InsuranceCatalogRepositoryPort;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.service.AffiliationService;
import com.fcv.citas.application.service.InsuranceCatalogService;
import com.fcv.citas.application.service.ProfessionalAdminService;
import com.fcv.citas.application.service.ProfileService;
import com.fcv.citas.application.service.SpecialtyService;
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

    @Bean
    InsuranceCatalogUseCase insuranceCatalogUseCase(InsuranceCatalogRepositoryPort repository,
                                                    TransactionPort transactions, Clock clock) {
        return new InsuranceCatalogService(repository, transactions, clock);
    }

    @Bean
    SpecialtyUseCase specialtyUseCase(SpecialtyRepositoryPort repository, TransactionPort transactions) {
        return new SpecialtyService(repository, transactions);
    }

    @Bean
    AffiliationUseCase affiliationUseCase(AffiliationRepositoryPort affiliations, InsuranceCatalogRepositoryPort catalog,
                                          TransactionPort transactions, Clock clock) {
        return new AffiliationService(affiliations, catalog, transactions, clock);
    }

    @Bean
    ProfessionalAdminUseCase professionalAdminUseCase(ProfessionalRepositoryPort professionals,
                                                      SpecialtyRepositoryPort specialties, PasswordHashPort passwords,
                                                      TransactionPort transactions, Clock clock) {
        return new ProfessionalAdminService(professionals, specialties, passwords, transactions, clock);
    }
}
