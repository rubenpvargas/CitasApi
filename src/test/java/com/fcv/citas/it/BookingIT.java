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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-016/HU-017 — reserva transaccional general y especializada contra MySQL real, incluida la carrera. */
class BookingIT extends AbstractMySqlIT {

    @Test
    void hu016GeneralBookingIsApprovedWithAuditAndAssignedSlot() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(5);
        long block = publishBlock(pro.token(), day, "08:00", "09:00", "HIC");

        JsonNode dto = body(general(userToken(), pro.id(), "HIC", day.atTime(8, 30))
                .andExpect(status().isCreated()).andReturn());

        assertThat(dto.get("status").asText()).isEqualTo("APPROVED");
        assertThat(dto.get("startAt").asText()).isEqualTo(day + "T08:30:00");
        assertThat(dto.get("endAt").asText()).isEqualTo(day + "T09:00:00");
        assertThat(dto.get("durationMinutes").asInt()).isEqualTo(30);
        assertThat(dto.get("specialtyName").asText()).isEqualTo("Medicina General");
        assertThat(dto.get("professionalName").asText()).isEqualTo("Valeria Agenda");
        assertThat(dto.get("locationName").asText()).isNotBlank();
        assertThat(dto.get("pendingReschedule").isNull()).isTrue();
        assertThat(dto.get("cancellable").asBoolean()).isTrue();
        assertThat(dto.get("reschedulable").asBoolean()).isTrue();
        long id = dto.get("id").asLong();
        Map<String, Object> history = jdbc.queryForMap("SELECT h.change_source, s.code, h.changed_by_user_id FROM "
                + "appointment_status_history h JOIN appointment_statuses s ON s.id = h.status_id WHERE h.appointment_id = ?", id);
        assertThat(history.get("change_source")).isEqualTo("USER");
        assertThat(history.get("code")).isEqualTo("APPROVED");
        assertThat(history.get("changed_by_user_id")).isNotNull();
        assertThat(jdbc.queryForList("SELECT DATE_FORMAT(start_at, '%H:%i') FROM professional_slots WHERE appointment_id = ?",
                String.class, id)).containsExactly("08:30");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM professional_slots WHERE availability_block_id = ? "
                + "AND appointment_id IS NULL", Integer.class, block)).isOne();
    }

    @Test
    void hu017SpecializedSixtyMinuteRequestHoldsTwoConsecutiveSlots() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("ORTOPEDIA_TRAUMATOLOGIA"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(5);
        publishBlock(pro.token(), day, "08:00", "10:00", "HIC");
        long ortho = specialtyId("ORTOPEDIA_TRAUMATOLOGIA");

        JsonNode dto = body(specialized(userToken(), ortho, pro.id(), "HIC", day.atTime(8, 30))
                .andExpect(status().isCreated()).andReturn());

        assertThat(dto.get("status").asText()).isEqualTo("REQUESTED");
        assertThat(dto.get("durationMinutes").asInt()).isEqualTo(60);
        assertThat(dto.get("reschedulable").asBoolean()).isFalse();
        assertThat(jdbc.queryForList("SELECT DATE_FORMAT(start_at, '%H:%i') FROM professional_slots WHERE appointment_id = ? "
                + "ORDER BY start_at", String.class, dto.get("id").asLong())).containsExactly("08:30", "09:00");
        // 08:00 queda libre pero sin un segundo slot consecutivo: otra solicitud de 60 min a las 08:00 no procede.
        specialized(userToken(), ortho, pro.id(), "HIC", day.atTime(8, 0))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        specialized(userToken(), specialtyId("MEDICINA_GENERAL"), pro.id(), "HIC", day.atTime(9, 30))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void revalidationFailuresAreRejectedWithoutCreatingAppointments() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(6);
        publishBlock(pro.token(), day, "08:00", "09:00", "HIC");
        String user = userToken();
        general(user, pro.id(), "HIC", day.atTime(8, 0)).andExpect(status().isCreated());

        general(user, pro.id(), "HIC", day.atTime(8, 0))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        general(user, pro.id(), "HIC", day.atTime(8, 15))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        general(user, pro.id(), "HIC", LocalDate.now().minusDays(1).atTime(8, 0))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLOT_UNAVAILABLE"));
        general(user, pro.id(), "ICV", day.atTime(8, 30))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LOCATION_NOT_ASSIGNED"));
        specialized(user, specialtyId("CARDIOLOGIA_ADULTO"), pro.id(), "HIC", day.atTime(8, 30))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SPECIALTY_NOT_ASSIGNED"));
        general(user, 987654L, "HIC", day.atTime(8, 30)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/appointments/general").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("professionalId", pro.id(),
                                "locationCode", "HIC", "startAt", "no-date"))))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE professional_id = ?", Integer.class,
                pro.id())).isOne();
    }

    @Test
    void onlyUsersCanBook() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(6);
        publishBlock(pro.token(), day, "10:00", "11:00", "HIC");

        general(tokenOnlyRoles("ADMIN"), pro.id(), "HIC", day.atTime(10, 0)).andExpect(status().isForbidden());
        general(pro.token(), pro.id(), "HIC", day.atTime(10, 0)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/appointments/general").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentBookingsOfTheSameSlotProduceExactlyOne201AndOne409() throws Exception {
        ProfessionalFixture pro = activeProfessional(List.of("MEDICINA_GENERAL"), List.of("HIC"));
        LocalDate day = agendaToday().plusDays(7);
        publishBlock(pro.token(), day, "08:00", "08:30", "HIC");
        List<String> users = List.of(userToken(), userToken());

        List<Integer> statuses = race(users, token -> general(token, pro.id(), "HIC", day.atTime(8, 0)));

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE professional_id = ?", Integer.class,
                pro.id())).isOne();
    }

    interface Call {
        ResultActions perform(String token) throws Exception;
    }

    /** Lanza ambas peticiones a la vez tras una barrera y devuelve sus estados HTTP. */
    List<Integer> race(List<String> tokens, Call call) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(tokens.size());
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String token : tokens) {
                Callable<Integer> task = () -> {
                    start.await();
                    return call.perform(token).andReturn().getResponse().getStatus();
                };
                futures.add(pool.submit(task));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    ResultActions general(String token, long professionalId, String location, LocalDateTime startAt) throws Exception {
        return mvc.perform(post("/api/v1/appointments/general").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("professionalId", professionalId, "locationCode", location,
                        "startAt", startAt + ":00", "reason", "Control sintetico"))));
    }

    ResultActions specialized(String token, long specialtyId, long professionalId, String location, LocalDateTime startAt)
            throws Exception {
        return mvc.perform(post("/api/v1/appointments/specialized").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("specialtyId", specialtyId, "professionalId", professionalId,
                        "locationCode", location, "startAt", startAt + ":00"))));
    }

    long specialtyId(String code) {
        return jdbc.queryForObject("SELECT id FROM specialties WHERE code = ?", Long.class, code);
    }
}
