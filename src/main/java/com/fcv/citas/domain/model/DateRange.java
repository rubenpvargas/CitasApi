package com.fcv.citas.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Rango de fechas de consulta de agenda: from y to obligatorios, ordenados y de máximo 31 días inclusive. */
public final class DateRange {
    public static final int MAX_DAYS = 31;

    public enum Violation { MISSING, INVERTED, TOO_LONG }

    private DateRange() {
    }

    public static Optional<Violation> check(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return Optional.of(Violation.MISSING);
        }
        if (to.isBefore(from)) {
            return Optional.of(Violation.INVERTED);
        }
        return ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS ? Optional.of(Violation.TOO_LONG) : Optional.empty();
    }
}
