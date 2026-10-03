package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.AvailabilityOffer;
import com.fcv.citas.application.model.AvailabilityQuery;

import java.util.List;

/** HU-015 — consulta de disponibilidad real para usuarios autenticados. */
public interface AvailabilityQueryUseCase {
    List<AvailabilityOffer> search(AvailabilityQuery query);
}
