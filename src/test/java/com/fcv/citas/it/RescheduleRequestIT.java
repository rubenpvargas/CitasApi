package com.fcv.citas.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-021 — solicitud de reprogramación: retención, original intacta, una PENDING, carrera por la franja. */
class RescheduleRequestIT extends AbstractMySqlIT {

    @Test
    void ca01Ca02RequestHoldsTheNewSlotAndKeepsTheOriginalAppointment() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC", "ICV"));
        LocalDate day = agendaToday().plusDays(5);
        publishBlock(pro.token(), day, "08:00", "08:30", "HIC");
        long target = publishBlock(pro.token(), day.plusDays(1), "10:00", "11:00", "ICV");
        String user = userToken();
        long id = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();

        JsonNode request = body(reschedule(user, id, day.plusDays(1).atTime(10, 30), "ICV", Map.of("professionalId", 1))
                .andExpect(status().isCreated()).andReturn());

        assertThat(request.get("status").asText()).isEqualTo("PENDING");
        assertThat(request.get("appointmentId").asLong()).isEqualTo(id);
        assertThat(request.get("requestedStartAt").asText()).isEqualTo(day.plusDays(1) + "T10:30:00");
        assertThat(request.get("requestedEndAt").asText()).isEqualTo(day.plusDays(1) + "T11:00:00");
        assertThat(request.get("locationCode").asText()).isEqualTo("ICV");
        long requestId = request.get("id").asLong();
        assertThat(jdbc.queryForList("SELECT DATE_FORMAT(start_at, '%H:%i') FROM professional_slots WHERE reschedule_request_id = ?",
                String.class, requestId)).containsExactly("10:30");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE availability_block_id = ? "
                + "AND appointment_id IS NULL AND reschedule_request_id IS NULL", Integer.class, target)).isOne();
        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", user))
                .andExpect(jsonPath("$.startAt").value(day + "T08:00:00"))
                .andExpect(jsonPath("$.locationCode").value("HIC"))
                .andExpect(jsonPath("$.pendingReschedule.id").value(requestId))
                .andExpect(jsonPath("$.pendingReschedule.locationCode").value("ICV"))
                .andExpect(jsonPath("$.reschedulable").value(false));
        assertThat(jdbc.queryForObject("SELECT change_source FROM reschedule_request_history WHERE reschedule_request_id = ?",
                String.class, requestId)).isEqualTo("USER");

        reschedule(user, id, day.plusDays(1).atTime(10, 0), "ICV", Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESCHEDULE_ALREADY_PENDING"));
    }

    @Test
    void ca03InvalidRequestsAreRejected() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL", "CARDIOLOGIA_ADULTO"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(6);
        publishBlock(pro.token(), day, "08:00", "10:00", "HIC");
        String user = userToken();
        long approved = bookGeneral(user, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long cardio = jdbc.queryForObject("SELECT id FROM specialties WHERE code = 'CARDIOLOGIA_ADULTO'", Long.class);
        long requested = body(mvc.perform(post("/api/v1/appointments/specialized").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("specialtyId", cardio,
                                "professionalId", pro.id(), "locationCode", "HIC", "startAt", day + "T08:30:00"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

        reschedule(user, requested, day.atTime(9, 0), "HIC", Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        reschedule(user, approved, day.atTime(8, 30), "HIC", Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        reschedule(user, approved, day.atTime(9, 0), "ICV", Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOCATION_NOT_ASSIGNED"));
        reschedule(userToken(), approved, day.atTime(9, 0), "HIC", Map.of()).andExpect(status().isNotFound());
        reschedule(user, approved, agendaToday().minusDays(1).atTime(9, 0), "HIC", Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reschedule_requests WHERE appointment_id IN (?, ?)",
                Integer.class, approved, requested)).isZero();
    }

    @Test
    void concurrentRequestsForTheSameTargetSlotProduceExactlyOne201AndOne409() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(7);
        publishBlock(pro.token(), day, "08:00", "09:00", "HIC");
        publishBlock(pro.token(), day, "12:00", "12:30", "HIC");
        String userA = userToken();
        String userB = userToken();
        long a = bookGeneral(userA, pro.id(), "HIC", day.atTime(8, 0)).get("id").asLong();
        long b = bookGeneral(userB, pro.id(), "HIC", day.atTime(8, 30)).get("id").asLong();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            futures.add(pool.submit(() -> { start.await();
                return reschedule(userA, a, day.atTime(12, 0), "HIC", Map.of()).andReturn().getResponse().getStatus(); }));
            futures.add(pool.submit(() -> { start.await();
                return reschedule(userB, b, day.atTime(12, 0), "HIC", Map.of()).andReturn().getResponse().getStatus(); }));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : futures) {
                statuses.add(f.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reschedule_requests WHERE appointment_id IN (?, ?)",
                Integer.class, a, b)).isOne();
    }

    ResultActions reschedule(String token, long appointmentId, LocalDateTime startAt, String location,
                             Map<String, Object> extra) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(extra);
        body.put("startAt", startAt + ":00");
        body.put("locationCode", location);
        return mvc.perform(post("/api/v1/appointments/" + appointmentId + "/reschedule").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }
}
