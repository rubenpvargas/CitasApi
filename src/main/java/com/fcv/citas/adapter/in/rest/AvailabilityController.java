package com.fcv.citas.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcv.citas.application.model.AvailabilityOffer;
import com.fcv.citas.application.model.AvailabilityQuery;
import com.fcv.citas.application.port.in.AvailabilityQueryUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** HU-015 — disponibilidad real (autenticado). Fechas/horas en hora local de pared sin offset. */
@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityController {
    private final AvailabilityQueryUseCase availability;

    public AvailabilityController(AvailabilityQueryUseCase availability) {
        this.availability = availability;
    }

    @GetMapping
    List<AvailabilityResponse> search(@RequestParam long specialtyId, @RequestParam LocalDate from,
                                      @RequestParam LocalDate to, @RequestParam(required = false) String locationCode,
                                      @RequestParam(required = false) Long professionalId) {
        return availability.search(new AvailabilityQuery(specialtyId, from, to, locationCode, professionalId)).stream()
                .map(AvailabilityResponse::from).toList();
    }

    /** {@code type} (GENERAL|SPECIALIZED) es aditivo y se deriva de specialty.general. */
    record AvailabilityResponse(long professionalId, String professionalName, long specialtyId, String specialtyName,
                                String locationCode, String locationName,
                                @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startAt,
                                @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endAt,
                                int durationMinutes, String type) {
        static AvailabilityResponse from(AvailabilityOffer o) {
            return new AvailabilityResponse(o.professionalId(), o.professionalName(), o.specialtyId(), o.specialtyName(),
                    o.locationCode(), o.locationName(), o.startAt(), o.endAt(), o.durationMinutes(),
                    o.general() ? "GENERAL" : "SPECIALIZED");
        }
    }
}
