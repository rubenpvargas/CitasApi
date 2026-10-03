package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.NotFoundException;
import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.TODAY;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.USER_ID;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.cmd;
import static com.fcv.citas.application.service.ProfessionalAdminServiceTest.assertCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-013 — edición con revalidación completa y baja lógica de bloques futuros no comprometidos. */
class AvailabilityBlockEditServiceTest {
    private final InMemoryProfessionals professionals = new InMemoryProfessionals();
    private final InMemoryBlocks blocks = new InMemoryBlocks(professionals);
    private final AvailabilityBlockService service = new AvailabilityBlockService(blocks, professionals,
            new DirectTransactions(), CLOCK);
    private static final long OTHER_USER = 60L;

    @BeforeEach
    void seed() {
        professionals.locations.put(1L, new Location(1L, "HIC", "Sede HIC", "a", "c", "d", true));
        professionals.locations.put(2L, new Location(2L, "ICV", "Sede ICV", "a", "c", "d", true));
        List<Location> both = List.of(professionals.locations.get(1L), professionals.locations.get(2L));
        professionals.professionals.put(10L, new ProfessionalSummary(10L, USER_ID, "V", "S", "v@x.test", "1", "P1", "L1",
                true, List.of(), both));
        professionals.professionals.put(11L, new ProfessionalSummary(11L, OTHER_USER, "O", "S", "o@x.test", "1", "P2", "L2",
                true, List.of(), both));
    }

    @Test
    void ca01EditRevalidatesAndRegeneratesSlots() {
        AvailabilityBlock block = service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC"));

        AvailabilityBlock edited = service.update(USER_ID, block.id(), cmd(TODAY.plusDays(2), "14:00", "16:00", "ICV"));

        assertThat(edited.locationCode()).isEqualTo("ICV");
        assertThat(edited.totalSlots()).isEqualTo(4);
        assertThat(blocks.rows.get(block.id()).slots).hasSize(4);
        assertThat(blocks.rows.get(block.id()).slots.getFirst().startAt()).isEqualTo(TODAY.plusDays(2).atTime(14, 0));
    }

    @Test
    void editKeepsHu012RulesIgnoringItselfForOverlap() {
        AvailabilityBlock a = service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "10:00", "HIC"));
        service.create(USER_ID, cmd(TODAY.plusDays(1), "10:00", "11:00", "ICV"));

        assertThat(service.update(USER_ID, a.id(), cmd(TODAY.plusDays(1), "08:30", "10:00", "HIC")).totalSlots()).isEqualTo(3);
        assertCode(() -> service.update(USER_ID, a.id(), cmd(TODAY.plusDays(1), "09:00", "10:30", "HIC")), "BLOCK_OVERLAP");
        assertCode(() -> service.update(USER_ID, a.id(), cmd(TODAY, "09:00", "10:00", "HIC")), "PAST_BLOCK");
        assertThatThrownBy(() -> service.update(USER_ID, a.id(), cmd(TODAY.plusDays(1), "08:20", "10:00", "HIC")))
                .isInstanceOf(RequestValidationException.class);
    }

    @Test
    void ca03PastCommittedOrForeignBlocksCannotChange() {
        AvailabilityBlock committed = service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC"));
        blocks.rows.get(committed.id()).committed = 1;
        AvailabilityBlock past = service.create(USER_ID, cmd(TODAY, "11:00", "12:00", "HIC"));
        blocks.rows.get(past.id()).schedule = new com.fcv.citas.domain.model.BlockSchedule(TODAY.minusDays(1),
                java.time.LocalTime.of(11, 0), java.time.LocalTime.of(12, 0));

        assertCode(() -> service.update(USER_ID, committed.id(), cmd(TODAY.plusDays(3), "08:00", "09:00", "HIC")), "BLOCK_COMMITTED");
        assertCode(() -> service.delete(USER_ID, committed.id()), "BLOCK_COMMITTED");
        assertCode(() -> service.update(USER_ID, past.id(), cmd(TODAY.plusDays(3), "08:00", "09:00", "HIC")), "PAST_BLOCK");
        assertCode(() -> service.delete(USER_ID, past.id()), "PAST_BLOCK");
        assertThatThrownBy(() -> service.update(OTHER_USER, committed.id(), cmd(TODAY.plusDays(3), "08:00", "09:00", "HIC")))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(OTHER_USER, committed.id())).isInstanceOf(NotFoundException.class);
        assertThat(blocks.rows.get(committed.id()).active).isTrue();
    }

    @Test
    void ca02DeleteIsLogicalAndRemovesFreeSlots() {
        AvailabilityBlock block = service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC"));

        service.delete(USER_ID, block.id());

        assertThat(blocks.rows.get(block.id()).active).isFalse();
        assertThat(blocks.rows.get(block.id()).slots).isEmpty();
        assertThatThrownBy(() -> service.delete(USER_ID, block.id())).isInstanceOf(NotFoundException.class);
    }
}
