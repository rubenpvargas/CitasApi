package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessRuleException;

/** Errores comunes de ocupación de franjas. */
final class SlotRules {
    private SlotRules() {
    }

    static BusinessRuleException unavailable() {
        return new BusinessRuleException("SLOT_UNAVAILABLE", "The selected time is not available");
    }
}
