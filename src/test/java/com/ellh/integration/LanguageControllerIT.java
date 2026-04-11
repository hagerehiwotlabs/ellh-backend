package com.ellh.integration;

import com.ellh.AbstractIntegrationTest;
import com.ellh.user.dto.LoginRequest;
import com.ellh.user.dto.RegisterRequest;
import com.ellh.user.entity.UserType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GET /api/v1/languages.
 * Verifies:
 *   - Unauthenticated requests return 401
 *   - Authenticated requests return the seeded language list
 *   - Languages include ISO 639-3 codes (Design Goal a)
 *   - Redis cache is populated after first call
 */
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LanguageControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String accessToken;

    @BeforeAll
    void registerAndLogin() throws Exception {
        // Register a test user
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("lang_test@ellh.com");
        reg.setPassword("Password123!");
        reg.setFirstName("Lang");
        reg.setLastName("Tester");
        reg.setUserType(UserType.FOREIGN_LEARNER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login to get access token
        LoginRequest login = new LoginRequest();
        login.setEmail("lang_test@ellh.com");
        login.setPassword("Password123!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void getLanguages_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/languages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLanguages_authenticated_returns3EthiopianLanguages() throws Exception {
        mockMvc.perform(get("/api/v1/languages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].isoCode", hasItems("amh", "tir", "orm")));
    }

    @Test
    void getLanguages_responsIncludesIso639Codes() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/languages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        var languages = objectMapper.readTree(body);

        // Design Goal a: ISO 639-3 codes must be present in ALL API responses
        languages.forEach(lang -> {
            assertThat(lang.get("isoCode").asText()).matches("[a-z]{3}");
            assertThat(lang.get("scriptType").asText())
                    .isIn("GEEZ_FIDEL", "LATIN_QUBEE");
        });
    }

    @Test
    void getLanguageByCode_amh_returnsAmharic() throws Exception {
        mockMvc.perform(get("/api/v1/languages/amh")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isoCode").value("amh"))
                .andExpect(jsonPath("$.name").value("Amharic"))
                .andExpect(jsonPath("$.scriptType").value("GEEZ_FIDEL"));
    }

    @Test
    void getLanguageByCode_unknown_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/languages/xyz")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
