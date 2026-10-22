package com.example.maintenance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks against the real Spring context and the seeded H2 database (dev profile).
 * If this class passes, the entity mappings, queries, security rules and the work order workflow
 * all work together.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void contextLoadsAndSeedDataIsPresent() throws Exception {
        String token = login("superintendent@example.com", "Super123!");
        mvc.perform(get("/api/v1/vessels").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
    }

    @Test
    void unauthenticatedRequestsAreRejectedWith401() throws Exception {
        mvc.perform(get("/api/v1/vessels")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejectedWith401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void engineerCannotCreateVesselsButSuperintendentCan() throws Exception {
        String body = "{\"name\":\"MV Test Ship\",\"imoNumber\":\"7654321\",\"vesselType\":\"CARGO\"}";

        mvc.perform(post("/api/v1/vessels").header("Authorization", "Bearer " + login("engineer@example.com", "Engineer123!"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        String superToken = login("superintendent@example.com", "Super123!");
        mvc.perform(post("/api/v1/vessels").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/vessels").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidImoNumberIsRejectedWith400() throws Exception {
        mvc.perform(post("/api/v1/vessels").header("Authorization", "Bearer " + login("admin@example.com", "Admin123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"imoNumber\":\"12\",\"vesselType\":\"TUG\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.imoNumber").exists());
    }

    @Test
    void dueScheduleListIncludesTheSeededOverdueTask() throws Exception {
        String token = login("engineer@example.com", "Engineer123!");
        mvc.perform(get("/api/v1/schedules/due").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dueStatus").value("OVERDUE"));
    }

    @Test
    void fullWorkOrderLifecycleEndsInAMaintenanceRecord() throws Exception {
        String superToken = login("superintendent@example.com", "Super123!");
        String engToken = login("engineer@example.com", "Engineer123!");
        long engineerId = userIdByEmail(superToken, "engineer@example.com");
        long equipmentId = firstEquipmentId(superToken);

        // create (open) -> assign
        String createBody = "{\"equipmentId\":" + equipmentId + ",\"title\":\"Integration test job\","
                + "\"type\":\"CORRECTIVE\",\"priority\":\"HIGH\"}";
        long id = idOf(mvc.perform(post("/api/v1/work-orders").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn());

        mvc.perform(post("/api/v1/work-orders/" + id + "/assign").header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + engineerId + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ASSIGNED"));

        // completing before starting is a workflow violation
        mvc.perform(post("/api/v1/work-orders/" + id + "/complete").header("Authorization", "Bearer " + engToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completionRemarks\":\"too early\"}"))
                .andExpect(status().isConflict());

        // start -> complete
        mvc.perform(post("/api/v1/work-orders/" + id + "/start").header("Authorization", "Bearer " + engToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(post("/api/v1/work-orders/" + id + "/complete").header("Authorization", "Bearer " + engToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionRemarks\":\"Repaired and tested\",\"actualHours\":1.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));

        // history + maintenance record exist
        mvc.perform(get("/api/v1/work-orders/" + id + "/history").header("Authorization", "Bearer " + engToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/v1/maintenance-records?equipmentId=" + equipmentId).header("Authorization", "Bearer " + engToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        // a finished order cannot be reopened
        mvc.perform(post("/api/v1/work-orders/" + id + "/start").header("Authorization", "Bearer " + engToken))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- helpers

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long userIdByEmail(String token, String email) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode user : mapper.readTree(result.getResponse().getContentAsString())) {
            if (email.equals(user.get("email").asText())) {
                return user.get("id").asLong();
            }
        }
        throw new IllegalStateException("Seeded user not found: " + email);
    }

    private long firstEquipmentId(String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/equipment?size=1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("content").get(0).get("id").asLong();
    }

    private long idOf(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
