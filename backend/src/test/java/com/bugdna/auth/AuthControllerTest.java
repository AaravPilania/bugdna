package com.bugdna.auth;

import com.bugdna.common.ApiExceptionHandler;
import com.bugdna.common.HealthController;
import com.bugdna.config.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private MockMvc authProtectedMockMvc;
    private DevTokenStore tokenStore;

    @BeforeEach
    void setUp() {
        tokenStore = new DevTokenStore();
        AuthController authController = new AuthController(tokenStore, "dev", "password123");
        HealthController healthController = new HealthController();

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController, healthController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        authProtectedMockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new ApiExceptionHandler())
                .addInterceptors(new AuthInterceptor(tokenStore))
                .build();
    }

    @Test
    @DisplayName("Health endpoint returns 200 without auth")
    void testHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 ProblemDetail")
    void testUnauthenticatedReturns401() throws Exception {
        authProtectedMockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Valid dev login returns 200 with Bearer token")
    void testValidLogin() throws Exception {
        String body = """
            {
              "username": "dev",
              "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.displayName").value("Lead Developer"));
    }

    @Test
    @DisplayName("Invalid login returns 401 ProblemDetail")
    void testInvalidLogin() throws Exception {
        String body = """
            {
              "username": "dev",
              "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Authenticated /api/v1/auth/me returns current user info")
    void testAuthMeWithToken() throws Exception {
        var tokenInfo = tokenStore.createToken("dev", "Lead Developer");

        authProtectedMockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenInfo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dev"))
                .andExpect(jsonPath("$.displayName").value("Lead Developer"));
    }
}
