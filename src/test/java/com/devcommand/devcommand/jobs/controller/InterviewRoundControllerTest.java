package com.devcommand.devcommand.jobs.controller;

import com.devcommand.devcommand.exception.GlobalExceptionHandler;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.jobs.dto.CreateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.dto.InterviewRoundResponse;
import com.devcommand.devcommand.jobs.entity.InterviewStatus;
import com.devcommand.devcommand.jobs.service.InterviewRoundService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InterviewRoundController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InterviewRoundControllerTest {

    private static final Long CALLER_ID = 1L;
    private static final Long JOB_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterviewRoundService interviewRoundService;

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

    private InterviewRoundResponse sampleResponse() {
        return new InterviewRoundResponse(
                50L, 1, "Technical", LocalDateTime.of(2026, 9, 28, 11, 0),
                InterviewStatus.SCHEDULED, null, "Prepare Spring Security"
        );
    }

    @Test
    void create_returns201WithCreatedRound() throws Exception {
        CreateInterviewRoundRequest request = new CreateInterviewRoundRequest(
                1, "Technical", LocalDateTime.of(2026, 9, 28, 11, 0),
                InterviewStatus.SCHEDULED, null, "Prepare Spring Security"
        );
        when(interviewRoundService.create(eq(JOB_ID), eq(CALLER_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/jobs/{jobId}/interviews", JOB_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.roundType").value("Technical"));
    }

    @Test
    void create_withBlankRoundType_returns400WithFieldError() throws Exception {
        CreateInterviewRoundRequest invalidRequest = new CreateInterviewRoundRequest(
                1, "", null, InterviewStatus.SCHEDULED, null, null
        );

        mockMvc.perform(post("/api/jobs/{jobId}/interviews", JOB_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.roundType").exists());
    }

    @Test
    void create_withNonPositiveRoundNumber_returns400WithFieldError() throws Exception {
        CreateInterviewRoundRequest invalidRequest = new CreateInterviewRoundRequest(
                0, "Technical", null, InterviewStatus.SCHEDULED, null, null
        );

        mockMvc.perform(post("/api/jobs/{jobId}/interviews", JOB_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.roundNumber").exists());
    }

    @Test
    void create_whenParentJobNotOwned_propagatesAs404() throws Exception {
        CreateInterviewRoundRequest request = new CreateInterviewRoundRequest(
                1, "Technical", null, InterviewStatus.SCHEDULED, null, null
        );
        when(interviewRoundService.create(eq(JOB_ID), eq(CALLER_ID), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResourceNotFoundException("Job application not found: " + JOB_ID));

        mockMvc.perform(post("/api/jobs/{jobId}/interviews", JOB_ID)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsRoundsForThatJob() throws Exception {
        when(interviewRoundService.getAllForJob(JOB_ID, CALLER_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/jobs/{jobId}/interviews", JOB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50));
    }

    @Test
    void getById_whenNotFound_propagatesAs404() throws Exception {
        when(interviewRoundService.getById(JOB_ID, 999L, CALLER_ID))
                .thenThrow(new ResourceNotFoundException("Interview round not found: 999"));

        mockMvc.perform(get("/api/jobs/{jobId}/interviews/{roundId}", JOB_ID, 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/jobs/{jobId}/interviews/{roundId}", JOB_ID, 50L))
                .andExpect(status().isNoContent());
    }
}
