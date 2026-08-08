package com.auction.platform;

import com.auction.platform.entity.User;
import com.auction.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    /**
     * Test-only helper that bypasses the actual email-click step by flipping the flag
     * directly via the repository. This mirrors what real test suites do for email-gated
     * flows — nobody parses SMTP output in an integration test.
     */
    void markEmailVerified(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Test
    void register_withValidPayload_returns201WithMessageOnly_noTokenUntilVerified() throws Exception {
        String payload = """
                {
                  "fullName": "Aditi Sharma",
                  "email": "aditi@example.com",
                  "password": "SecureP@ss1",
                  "phoneNumber": "+919812345678"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value(
                        "Registration successful. Please check your email to verify your account before logging in."));
    }

    @Test
    void login_beforeEmailVerification_returns403() throws Exception {
        String registerPayload = """
                {
                  "fullName": "Unverified User",
                  "email": "unverified@example.com",
                  "password": "SecureP@ss1",
                  "phoneNumber": "+919812345699"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerPayload))
                .andExpect(status().isCreated());

        String loginPayload = """
                {
                  "email": "unverified@example.com",
                  "password": "SecureP@ss1"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please verify your email before logging in."));
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        String payload = """
                {
                  "fullName": "Rahul Verma",
                  "email": "duplicate@example.com",
                  "password": "SecureP@ss1",
                  "phoneNumber": "+919812345679"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_withWeakPassword_returns400() throws Exception {
        String payload = """
                {
                  "fullName": "Weak Pass",
                  "email": "weak@example.com",
                  "password": "weak",
                  "phoneNumber": "+919812345670"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void login_afterEmailVerification_returns200WithAccessAndRefreshToken() throws Exception {
        String registerPayload = """
                {
                  "fullName": "Login User",
                  "email": "loginuser@example.com",
                  "password": "SecureP@ss1",
                  "phoneNumber": "+919812345671"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerPayload))
                .andExpect(status().isCreated());

        markEmailVerified("loginuser@example.com");

        String loginPayload = """
                {
                  "email": "loginuser@example.com",
                  "password": "SecureP@ss1"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_BUYER"));
    }

    @Test
    void login_withWrongPassword_returns401AndDoesNotLeakWhichFieldWasWrong() throws Exception {
        String registerPayload = """
                {
                  "fullName": "Bad Login User",
                  "email": "badlogin@example.com",
                  "password": "SecureP@ss1",
                  "phoneNumber": "+919812345672"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerPayload))
                .andExpect(status().isCreated());

        markEmailVerified("badlogin@example.com");

        String loginPayload = """
                {
                  "email": "badlogin@example.com",
                  "password": "WrongPassword1!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refreshToken_afterRotation_oldTokenIsRejectedAsReuse() throws Exception {
        String registerPayload = """
                {
                  "fullName": "Refresh User",
                  "email": "refreshuser@example.com",
                  "password": "SecureP@ss1",
                  "phoneNumber": "+919812345673"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerPayload))
                .andExpect(status().isCreated());

        markEmailVerified("refreshuser@example.com");

        String loginPayload = """
                {
                  "email": "refreshuser@example.com",
                  "password": "SecureP@ss1"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String originalRefreshToken = objectMapper.readTree(loginResponse)
                .get("data").get("refreshToken").asText();

        String refreshPayload = """
                { "refreshToken": "%s" }
                """.formatted(originalRefreshToken);

        // First use: rotates successfully
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType("application/json")
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        // Reuse of the now-revoked original token: rejected
        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType("application/json")
                        .content(refreshPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPassword_returnsSameGenericMessage_regardlessOfWhetherEmailExists() throws Exception {
        String registerPayload = """
                {
                  "fullName": "Forgot Pw User",
                  "email": "forgotpw@example.com",
                  "password": "SecureP@ss1",
                  "phoneNumber": "+919812345674"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerPayload))
                .andExpect(status().isCreated());

        String existingEmailPayload = """
                { "email": "forgotpw@example.com" }
                """;
        String nonExistentEmailPayload = """
                { "email": "nobody-registered@example.com" }
                """;

        String expectedMessage = "If that email is registered, a reset link has been sent.";

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(existingEmailPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value(expectedMessage));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType("application/json")
                        .content(nonExistentEmailPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value(expectedMessage));
    }

    @Test
    void resetPassword_withInvalidToken_returns401() throws Exception {
        String payload = """
                { "token": "not-a-real-token", "newPassword": "NewSecureP@ss2" }
                """;

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid password reset token"));
    }

    @Test
    void verifyEmail_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify-email").param("token", "not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid verification token"));
    }
}
