package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.RequestValidationException;
import com.fcv.citas.domain.model.DateRange;

import java.time.LocalDate;

/** Traduce las violaciones de {@link DateRange} a 400 VALIDATION_ERROR. */
final class DateRanges {
    private DateRanges() {
    }

    static void require(LocalDate from, LocalDate to) {
        DateRange.check(from, to).ifPresent(violation -> {
            throw switch (violation) {
                case MISSING -> new RequestValidationException(from == null ? "from" : "to", "is required");
                case INVERTED -> new RequestValidationException("to", "must not be before from");
                case TOO_LONG -> new RequestValidationException("to", "range must not exceed " + DateRange.MAX_DAYS + " days");
            };
        });
    }
}
