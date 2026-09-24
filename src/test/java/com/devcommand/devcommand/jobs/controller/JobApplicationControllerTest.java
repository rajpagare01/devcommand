package com.devcommand.devcommand.jobs.controller;

import com.devcommand.devcommand.exception.GlobalExceptionHandler;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.dto.JobStatusUpdateRequest;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
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
 * Same slice-test approach as DsaProblemControllerTest/DailyTaskControllerTest:
 * addFilters=false skips the real security filter chain, and the caller's
 * identity is pushed directly into SecurityContextHolder. Real "no token ->
 * 401/403" enforcement is exercised by the Postman collection's security
 * folder against the running application.
 */
@WebMvcTest(controllers = JobApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class JobApplicationControllerTest {

    private static final Long CALLER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobApplicationService jobApplicationService;

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

    private JobApplicationResponse sampleResponse() {
        return new JobApplicationResponse(
                10L, "TCS", "Java Developer", "Indore", "https://example.com/job", "LinkedIn",
                "6-8 LPA", LocalDate.of(2026, 9, 24), ApplicationStatus.APPLIED, "Java + Spring Boot role",
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void create_returns201WithCreatedJob() throws Exception {
        CreateJobApplicationRequest request = new CreateJobApplicationRequest(
                "TCS", "Java Developer", "Indore", "https://example.com/job", "LinkedIn",
                "6-8 LPA", LocalDate.of(2026, 9, 24), ApplicationStatus.APPLIED, "Java + Spring Boot role"
        );
        when(jobApplicationService.create(any(), eq(CALLER_ID))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.company").value("TCS"));
    }

    @Test
    void create_withBlankCompany_returns400WithFieldError() throws Exception {
        CreateJobApplicationRequest invalidRequest = new CreateJobApplicationRequest(
                "", "Java Developer", null, null, null, null,
                LocalDate.of(2026, 9, 24), ApplicationStatus.APPLIED, null
        );

        mockMvc.perform(post("/api/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.company").exists());
    }

    @Test
    void create_withMissingApplicationDate_returns400WithFieldError() throws Exception {
        String bodyMissingDate = """
                {
                  "company": "TCS",
                  "role": "Java Developer",
                  "status": "APPLIED"
                }
                """;

        mockMvc.perform(post("/api/jobs")
                        .contentType("application/json")
                        .content(bodyMissingDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.applicationDate").exists());
    }

    @Test
    void getAll_returnsPageForTheAuthenticatedUser() throws Exception {
        Page<JobApplicationResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(jobApplicationService.getAll(eq(CALLER_ID), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void getById_whenServiceThrowsNotFound_propagatesAs404() throws Exception {
        when(jobApplicationService.getById(eq(999L), eq(CALLER_ID)))
                .thenThrow(new ResourceNotFoundException("Job application not found: 999"));

        mockMvc.perform(get("/api/jobs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/jobs/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void changeStatus_returns200WithUpdatedStatus() throws Exception {
        JobApplicationResponse updated = new JobApplicationResponse(
                10L, "TCS", "Java Developer", "Indore", "https://example.com/job", "LinkedIn",
                "6-8 LPA", LocalDate.of(2026, 9, 24), ApplicationStatus.INTERVIEW, "Java + Spring Boot role",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(jobApplicationService.changeStatus(eq(10L), eq(CALLER_ID), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/jobs/10/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new JobStatusUpdateRequest(ApplicationStatus.INTERVIEW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void changeStatus_withMissingStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/jobs/10/status")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }
}
