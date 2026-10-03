package com.devcommand.devcommand.dsa.controller;

import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import com.devcommand.devcommand.exception.GlobalExceptionHandler;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * addFilters=false skips JwtAuthenticationFilter/SecurityConfig entirely
 * (this is a slice test, not a full integration test) - the caller's
 * identity is instead pushed straight into SecurityContextHolder, which is
 * exactly what @AuthenticationPrincipal reads regardless of which filter
 * populated it. That keeps this test focused on "does the controller wire
 * HTTP <-> the service correctly for the right user", while
 * DsaProblemServiceTest covers ownership enforcement itself, and the
 * Postman collection's security folder exercises the real filter chain
 * ("no token -> 401/403") against the running application.
 */
@WebMvcTest(controllers = DsaProblemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DsaProblemControllerTest {

    private static final Long CALLER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.devcommand.devcommand.security.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private com.devcommand.devcommand.ratelimit.service.RateLimitService rateLimitService;

    @MockBean
    private com.devcommand.devcommand.metrics.DevCommandMetrics devCommandMetrics;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DsaProblemService dsaProblemService;

    @BeforeEach
    void authenticateAsCaller() {
        User user = User.builder().id(CALLER_ID).name("Ada").email("ada@example.com").password("hash").build();
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private DsaProblemResponse sampleResponse() {
        return new DsaProblemResponse(
                10L, "Two Sum", "LEETCODE", "https://leetcode.com/problems/two-sum/", "ARRAY",
                Difficulty.EASY, ProblemStatus.SOLVED, LocalDate.of(2026, 9, 23), 25,
                "Solved with HashMap", LocalDate.of(2026, 9, 30),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void create_returns201WithCreatedProblem() throws Exception {
        CreateDsaProblemRequest request = new CreateDsaProblemRequest(
                "Two Sum", "LEETCODE", "https://leetcode.com/problems/two-sum/", "ARRAY",
                Difficulty.EASY, ProblemStatus.SOLVED, LocalDate.of(2026, 9, 23), 25,
                "Solved with HashMap", LocalDate.of(2026, 9, 30)
        );
        when(dsaProblemService.create(any(), eq(CALLER_ID))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/dsa")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Two Sum"));
    }

    @Test
    void create_withBlankTitle_returns400WithFieldError() throws Exception {
        CreateDsaProblemRequest invalidRequest = new CreateDsaProblemRequest(
                "", "LEETCODE", null, "ARRAY", Difficulty.EASY, ProblemStatus.TODO,
                null, null, null, null
        );

        mockMvc.perform(post("/api/dsa")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void getAll_returnsPageForTheAuthenticatedUser() throws Exception {
        Page<DsaProblemResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(dsaProblemService.getAll(eq(CALLER_ID), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/dsa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void getById_whenServiceThrowsNotFound_propagatesAs404() throws Exception {
        when(dsaProblemService.getById(eq(999L), eq(CALLER_ID)))
                .thenThrow(new ResourceNotFoundException("DSA problem not found: 999"));

        mockMvc.perform(get("/api/dsa/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/dsa/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void solve_returns200WithUpdatedStatus() throws Exception {
        when(dsaProblemService.markSolved(eq(10L), eq(CALLER_ID))).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/dsa/10/solve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLVED"));
    }

    @Test
    void revision_returns200() throws Exception {
        when(dsaProblemService.markForRevision(eq(10L), eq(CALLER_ID))).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/dsa/10/revision"))
                .andExpect(status().isOk());
    }
}

