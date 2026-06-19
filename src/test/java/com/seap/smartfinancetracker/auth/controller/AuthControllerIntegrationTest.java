package com.seap.smartfinancetracker.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.auth.dto.LoginRequest;
import com.seap.smartfinancetracker.auth.dto.RegisterRequest;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EMAIL = "integration@example.com";
    private static final String PASSWORD = "StrongPass123";
    private static final String FULL_NAME = "Integration User";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private void register(RegisterRequest request) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
    //</editor-fold>

    //<editor-fold desc="POST /auth/register">
    @Test
    void shouldRegisterUserAndReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    void shouldRejectRegistration_WhenEmailAlreadyExists() throws Exception {
        register(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME));

        // Second registration with the same email must conflict
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(EMAIL, PASSWORD, "Someone Else"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("AUTH-409-01"));
    }

    @Test
    void shouldRejectRegistration_WhenEmailFormatIsInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("not-an-email", PASSWORD, FULL_NAME);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectRegistration_WhenPasswordTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest(EMAIL, "short", FULL_NAME);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectRegistration_WhenRequiredFieldsMissing() throws Exception {
        // Blank email, password and full name all violate @NotBlank
        RegisterRequest request = new RegisterRequest("", "", "");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
    //</editor-fold>

    //<editor-fold desc="POST /auth/login">
    @Test
    void shouldLoginSuccessfully_WithValidCredentials() throws Exception {
        register(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME));

        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    void shouldRejectLogin_WhenPasswordIsWrong() throws Exception {
        register(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME));

        LoginRequest request = new LoginRequest(EMAIL, "WrongPassword123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH-401-01"));
    }

    @Test
    void shouldRejectLogin_WhenUserDoesNotExist() throws Exception {
        LoginRequest request = new LoginRequest("missing@example.com", PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH-401-01"));
    }
    //</editor-fold>
}
