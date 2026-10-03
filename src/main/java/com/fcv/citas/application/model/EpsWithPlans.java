package com.fcv.citas.application.model;

import com.fcv.citas.domain.model.Eps;
import com.fcv.citas.domain.model.EpsPlan;

import java.util.List;

public record EpsWithPlans(Eps eps, List<EpsPlan> plans) {
}
