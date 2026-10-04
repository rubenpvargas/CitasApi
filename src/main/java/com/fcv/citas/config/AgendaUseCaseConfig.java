package com.fcv.citas.config;

import com.fcv.citas.application.port.in.AdminInboxUseCase;
import com.fcv.citas.application.port.in.AffiliationUseCase;
import com.fcv.citas.application.port.in.AppointmentDecisionUseCase;
import com.fcv.citas.application.port.in.AvailabilityBlockUseCase;
import com.fcv.citas.application.port.in.AvailabilityQueryUseCase;
import com.fcv.citas.application.port.in.BookingUseCase;
import com.fcv.citas.application.port.in.MyAppointmentsUseCase;
import com.fcv.citas.application.port.in.InsuranceCatalogUseCase;
import com.fcv.citas.application.port.in.ProfessionalAdminUseCase;
import com.fcv.citas.application.port.in.ProfessionalAgendaUseCase;
import com.fcv.citas.application.port.in.ProfileUseCase;
import com.fcv.citas.application.port.in.ReminderQueryUseCase;
import com.fcv.citas.application.port.in.RescheduleUseCase;
import com.fcv.citas.application.port.in.SpecialtyUseCase;
import com.fcv.citas.application.port.out.AffiliationRepositoryPort;
import com.fcv.citas.application.port.out.AvailabilityBlockRepositoryPort;
import com.fcv.citas.application.port.out.AppointmentRepositoryPort;
import com.fcv.citas.application.port.out.AvailabilityQueryPort;
import com.fcv.citas.application.port.out.RescheduleRepositoryPort;
import com.fcv.citas.application.port.out.SlotRepositoryPort;
import com.fcv.citas.application.port.out.InsuranceCatalogRepositoryPort;
import com.fcv.citas.application.port.out.PasswordHashPort;
import com.fcv.citas.application.port.out.ProfessionalRepositoryPort;
import com.fcv.citas.application.port.out.ProfileRepositoryPort;
import com.fcv.citas.application.port.out.SpecialtyRepositoryPort;
import com.fcv.citas.application.port.out.TransactionPort;
import com.fcv.citas.application.service.AdminInboxService;
import com.fcv.citas.application.service.AffiliationService;
import com.fcv.citas.application.service.AppointmentDecisionService;
import com.fcv.citas.application.service.AvailabilityBlockService;
import com.fcv.citas.application.service.AvailabilityQueryService;
import com.fcv.citas.application.service.BookingService;
import com.fcv.citas.application.service.MyAppointmentsService;
import com.fcv.citas.application.service.InsuranceCatalogService;
import com.fcv.citas.application.service.ProfessionalAdminService;
import com.fcv.citas.application.service.ProfessionalAgendaService;
import com.fcv.citas.application.service.ProfileService;
import com.fcv.citas.application.service.ReminderQueryService;
import com.fcv.citas.application.service.RescheduleService;
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

    @Bean
    AvailabilityBlockUseCase availabilityBlockUseCase(AvailabilityBlockRepositoryPort blocks,
                                                      ProfessionalRepositoryPort professionals,
                                                      TransactionPort transactions, Clock clock) {
        return new AvailabilityBlockService(blocks, professionals, transactions, clock);
    }

    @Bean
    AvailabilityQueryUseCase availabilityQueryUseCase(AvailabilityQueryPort query, SpecialtyRepositoryPort specialties,
                                                      Clock clock) {
        return new AvailabilityQueryService(query, specialties, clock);
    }

    @Bean
    BookingUseCase bookingUseCase(AppointmentRepositoryPort appointments, SlotRepositoryPort slots,
                                  ProfessionalRepositoryPort professionals, SpecialtyRepositoryPort specialties,
                                  TransactionPort transactions, Clock clock) {
        return new BookingService(appointments, slots, professionals, specialties, transactions, clock);
    }

    @Bean
    AppointmentDecisionUseCase appointmentDecisionUseCase(AppointmentRepositoryPort appointments, SlotRepositoryPort slots,
                                                          TransactionPort transactions, Clock clock) {
        return new AppointmentDecisionService(appointments, slots, transactions, clock);
    }

    @Bean
    MyAppointmentsUseCase myAppointmentsUseCase(AppointmentRepositoryPort appointments, SlotRepositoryPort slots,
                                                RescheduleRepositoryPort reschedules, TransactionPort transactions,
                                                Clock clock) {
        return new MyAppointmentsService(appointments, slots, reschedules, transactions, clock);
    }

    @Bean
    RescheduleUseCase rescheduleUseCase(AppointmentRepositoryPort appointments, RescheduleRepositoryPort reschedules,
                                        SlotRepositoryPort slots, ProfessionalRepositoryPort professionals,
                                        SpecialtyRepositoryPort specialties, TransactionPort transactions, Clock clock) {
        return new RescheduleService(appointments, reschedules, slots, professionals, specialties, transactions, clock);
    }

    @Bean
    ProfessionalAgendaUseCase professionalAgendaUseCase(AppointmentRepositoryPort appointments,
                                                        ProfessionalRepositoryPort professionals,
                                                        TransactionPort transactions, Clock clock) {
        return new ProfessionalAgendaService(appointments, professionals, transactions, clock);
    }

    @Bean
    AdminInboxUseCase adminInboxUseCase(AppointmentRepositoryPort appointments, RescheduleRepositoryPort reschedules) {
        return new AdminInboxService(appointments, reschedules);
    }

    @Bean
    ReminderQueryUseCase reminderQueryUseCase(AppointmentRepositoryPort appointments, Clock clock) {
        return new ReminderQueryService(appointments, clock);
    }
}
