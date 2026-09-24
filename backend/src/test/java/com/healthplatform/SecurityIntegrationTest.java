package com.healthplatform;

import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.repository.UserRepository;
import com.healthplatform.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack (real filter chain, H2) checks of the production-hardening guarantees: default-deny,
 * no privilege escalation via /register, admin-only audit API, security headers, CORS allow-list.
 * Redis is mocked; the rate limiter fails open without it, which is exactly what is exercised here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @MockBean private StringRedisTemplate redisTemplate;

    private String adminToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        adminToken = tokenFor(Role.ADMIN);
        doctorToken = tokenFor(Role.DOCTOR);
    }

    private String tokenFor(Role role) {
        User user = userRepository.save(User.builder()
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@x.test")
                .passwordHash("not-used")
                .role(role)
                .build());
        return jwtService.generateAccessToken(user);
    }

    @Test
    void unauthenticatedApiCall_is401() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCannotRegisterAnAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"evil@x.test\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCannotRegisterAnAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"evil2@x.test\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanRegisterStaff() throws Exception {
        mvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new-doc-" + UUID.randomUUID() + "@x.test\",\"password\":\"Password123!\",\"role\":\"DOCTOR\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void anonymousCanSelfRegisterAsPatient() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pat-" + UUID.randomUUID() + "@x.test\",\"password\":\"Password123!\",\"role\":\"PATIENT\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void auditApi_isAdminOnly() throws Exception {
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + doctorToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
    }

    @Test
    void authenticatedApiCallsAreWrittenToTheAuditTrail() throws Exception {
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

        mvc.perform(get("/api/audit").param("resourceType", "audit").param("resourceId", "none")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        // The first call above must now be visible in the trail (newest first).
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.content[0].resourceType").value("audit"))
                .andExpect(jsonPath("$.content[0].action").value("READ"));
    }

    @Test
    void mfaChallengeTokenIsNotAcceptedAsAnAccessToken() throws Exception {
        User user = userRepository.save(User.builder().email("mfa-" + UUID.randomUUID() + "@x.test")
                .passwordHash("x").role(Role.ADMIN).build());

        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + jwtService.generateMfaChallengeToken(user)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responsesCarrySecurityHeadersAndARequestId() throws Exception {
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + adminToken))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void cors_allowsConfiguredOriginOnly() throws Exception {
        mvc.perform(options("/api/patients").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(options("/api/patients").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actuatorPrometheusIsNotPublicOnTheMainPort() throws Exception {
        // MockMvc reports local port 80 (not the configured management port), so this exercises the
        // "public port" rule: only /actuator/health is open; everything else needs an ADMIN.
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }
}
