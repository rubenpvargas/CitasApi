package com.fcv.citas.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Decisión de tiempo: consultas por rango exigen from/to con máximo 31 días (inclusive). */
class DateRangeTest {
    private static final LocalDate D = LocalDate.of(2026, 10, 1);

    @Test
    void acceptsSingleDayAndThirtyOneDayRanges() {
        assertThat(DateRange.check(D, D)).isEmpty();
        assertThat(DateRange.check(D, D.plusDays(30))).isEmpty();
    }

    @Test
    void rejectsMissingInvertedAndLongerThanThirtyOneDays() {
        assertThat(DateRange.check(null, D)).contains(DateRange.Violation.MISSING);
        assertThat(DateRange.check(D, D.minusDays(1))).contains(DateRange.Violation.INVERTED);
        assertThat(DateRange.check(D, D.plusDays(31))).contains(DateRange.Violation.TOO_LONG);
    }
}
