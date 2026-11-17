package com.bugdna.bug;

import com.bugdna.auth.DevTokenStore;
import com.bugdna.bug.dto.BugDetailDto;
import com.bugdna.bug.dto.BugPatchRequest;
import com.bugdna.common.ApiExceptionHandler;
import com.bugdna.config.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BugControllerTest {

    private MockMvc mockMvc;
    private BugService bugService;
    private BugOccurrenceRepository bugOccurrenceRepository;
    private DevTokenStore tokenStore;
    private String validToken;

    @BeforeEach
    void setUp() {
        tokenStore = new DevTokenStore();
        var tokenInfo = tokenStore.createToken("dev", "Lead Developer");
        validToken = tokenInfo.token();

        bugService = mock(BugService.class);
        bugOccurrenceRepository = mock(BugOccurrenceRepository.class);

        BugController controller = new BugController(bugService, bugOccurrenceRepository);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .addInterceptors(new AuthInterceptor(tokenStore))
                .build();
    }

    @Test
    @DisplayName("PATCH /api/v1/bugs/104 with valid token updates status successfully")
    void testPatchStatusAuthenticated() throws Exception {
        BugDetailDto mockDetail = new BugDetailDto(
                104L, 1L, "87564c7cca08eb024a86fd2954cd906551d6149d061490bf46a97c597706e968",
                "POST /demo-api/cart/coupon → 400 INVALID_COUPON_FORMAT",
                ErrorType.API_ERROR, "INVALID_COUPON_FORMAT", "POST", "/demo-api/cart/coupon", "/cart",
                BugStatus.IN_PROGRESS, BugPriority.HIGH, "Aarav", 4, false, 0,
                LocalDateTime.now().minusHours(1), LocalDateTime.now(), null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now(), 2, 1, 60L
        );

        when(bugService.patchBug(eq(104L), any(BugPatchRequest.class))).thenReturn(mockDetail);

        String patchJson = """
            {
              "status": "IN_PROGRESS",
              "priority": "HIGH",
              "assignee": "Aarav"
            }
            """;

        mockMvc.perform(patch("/api/v1/bugs/104")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(104))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignee").value("Aarav"));
    }

    @Test
    @DisplayName("PATCH with invalid enum value returns 400 ProblemDetail")
    void testPatchInvalidEnumReturns400() throws Exception {
        String invalidJson = """
            {
              "status": "NOT_A_VALID_STATUS"
            }
            """;

        mockMvc.perform(patch("/api/v1/bugs/104")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("PATCH without token returns 401 ProblemDetail")
    void testPatchUnauthenticatedReturns401() throws Exception {
        String patchJson = """
            {
              "status": "IN_PROGRESS"
            }
            """;

        mockMvc.perform(patch("/api/v1/bugs/104")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}
