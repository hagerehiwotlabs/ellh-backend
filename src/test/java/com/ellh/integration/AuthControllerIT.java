package com.ellh.integration;

import com.ellh.AbstractIntegrationTest;
import com.ellh.user.dto.LoginRequest;
import com.ellh.user.dto.RegisterRequest;
import com.ellh.user.entity.UserType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for POST /api/v1/auth/{register,login,refresh}.
 * Uses Testcontainers (PostgreSQL + Redis + MongoDB) via AbstractIntegrationTest.
 * Flyway migrations run automatically on context startup — no manual setup needed.
 *
 * These tests verify the full stack end-to-end:
 *   HTTP request → Controller → Service → Repository → Database → HTTP response
 */
@AutoConfigureMockMvc
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL     = "/api/v1/auth/login";
    private static final String REFRESH_URL   = "/api/v1/auth/refresh";

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithTokens() throws Exception {
        RegisterRequest request = buildRegisterRequest("register_test@ellh.com");

        MvcResult result = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("register_test@ellh.com"))
                .andExpect(jsonPath("$.onboardingComplete").value(false))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("accessToken");
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = buildRegisterRequest("duplicate@ellh.com");

        // First registration — should succeed
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email — should fail
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void register_invalidEmail_returns400WithFieldError() throws Exception {
        RegisterRequest request = buildRegisterRequest("not-an-email");

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        RegisterRequest request = buildRegisterRequest("test2@ellh.com");
        request.setPassword(null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        // Register first
        RegisterRequest reg = buildRegisterRequest("login_test@ellh.com");
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("login_test@ellh.com");
        loginReq.setPassword("Password123!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login_test@ellh.com"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        RegisterRequest reg = buildRegisterRequest("wrong_pw@ellh.com");
        mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("wrong_pw@ellh.com");
        loginReq.setPassword("WrongPassword!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validRefreshToken_returns200WithNewAccessToken() throws Exception {
        // Register and capture the refresh token
        RegisterRequest reg = buildRegisterRequest("refresh_test@ellh.com");
        MvcResult regResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        String regBody = regResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(regBody).get("refreshToken").asText();

        // Use refresh token to get new access token
        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        String body = "{\"refreshToken\":\"invalid.token.here\"}";
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RegisterRequest buildRegisterRequest(String email) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword("Password123!");
        r.setFirstName("Abebe");
        r.setLastName("Kebede");
        r.setUserType(UserType.FOREIGN_LEARNER);
        return r;
    }
}
