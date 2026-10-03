package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.application.model.CalendarEntry;
import com.fcv.citas.domain.model.AvailabilityBlock;
import com.fcv.citas.domain.model.BlockLockReason;
import com.fcv.citas.domain.model.BlockSchedule;
import com.fcv.citas.domain.model.Location;
import com.fcv.citas.domain.model.ProfessionalSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.CLOCK;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.TODAY;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.USER_ID;
import static com.fcv.citas.application.service.AvailabilityBlockServiceTest.cmd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU-014 — calendario propio por rango y sede, con ocupación y motivo de no edición. */
class CalendarServiceTest {
    private final InMemoryProfessionals professionals = new InMemoryProfessionals();
    private final InMemoryBlocks blocks = new InMemoryBlocks(professionals);
    private final AvailabilityBlockService service = new AvailabilityBlockService(blocks, professionals,
            new DirectTransactions(), CLOCK);

    @BeforeEach
    void seed() {
        professionals.locations.put(1L, new Location(1L, "HIC", "Sede HIC", "a", "c", "d", true));
        professionals.locations.put(2L, new Location(2L, "ICV", "Sede ICV", "a", "c", "d", true));
        List<Location> both = List.of(professionals.locations.get(1L), professionals.locations.get(2L));
        professionals.professionals.put(10L, new ProfessionalSummary(10L, USER_ID, "V", "S", "v@x.test", "1", "P1", "L1",
                true, List.of(), both));
        professionals.professionals.put(11L, new ProfessionalSummary(11L, 61L, "O", "S", "o@x.test", "1", "P2", "L2",
                true, List.of(), both));
    }

    @Test
    void ca01ListsOwnBlocksInRangeWithOccupancyAndEditability() {
        AvailabilityBlock free = service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC"));
        AvailabilityBlock committed = service.create(USER_ID, cmd(TODAY.plusDays(2), "08:00", "09:00", "ICV"));
        AvailabilityBlock past = service.create(USER_ID, cmd(TODAY, "11:00", "12:00", "HIC"));
        blocks.rows.get(committed.id()).committed = 1;
        blocks.rows.get(past.id()).schedule = new BlockSchedule(TODAY, LocalTime.of(8, 0), LocalTime.of(9, 0));
        service.create(USER_ID, cmd(TODAY.plusDays(40), "08:00", "09:00", "HIC"));
        service.create(61L, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC"));

        List<CalendarEntry> entries = service.calendar(USER_ID, TODAY, TODAY.plusDays(30), null);

        assertThat(entries).extracting(e -> e.block().id()).containsExactly(past.id(), free.id(), committed.id());
        assertThat(entries.get(0).notEditableReason()).contains(BlockLockReason.PAST_BLOCK);
        assertThat(entries.get(1).editable()).isTrue();
        assertThat(entries.get(2).notEditableReason()).contains(BlockLockReason.BLOCK_COMMITTED);
        assertThat(entries.get(2).block().committedSlots()).isOne();
    }

    @Test
    void filtersByLocationCode() {
        service.create(USER_ID, cmd(TODAY.plusDays(1), "08:00", "09:00", "HIC"));
        service.create(USER_ID, cmd(TODAY.plusDays(1), "10:00", "11:00", "ICV"));

        assertThat(service.calendar(USER_ID, TODAY, TODAY.plusDays(5), "ICV"))
                .extracting(e -> e.block().locationCode()).containsExactly("ICV");
    }

    @Test
    void rangeMustBeOrderedAndAtMostThirtyOneDays() {
        assertThatThrownBy(() -> service.calendar(USER_ID, TODAY, TODAY.minusDays(1), null))
                .isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.calendar(USER_ID, TODAY, TODAY.plusDays(31), null))
                .isInstanceOf(RequestValidationException.class).extracting("field").isEqualTo("to");
    }
}
