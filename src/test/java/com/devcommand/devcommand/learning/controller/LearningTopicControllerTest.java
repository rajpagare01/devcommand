package com.devcommand.devcommand.learning.controller;

import com.devcommand.devcommand.exception.GlobalExceptionHandler;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.learning.dto.CreateLearningTopicRequest;
import com.devcommand.devcommand.learning.dto.LearningTopicResponse;
import com.devcommand.devcommand.learning.dto.ProgressUpdateRequest;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.service.LearningTopicService;
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
 * Same slice-test approach as the DSA/Tasks/Jobs controller tests:
 * addFilters=false skips the real security filter chain, and the caller's
 * identity is pushed directly into SecurityContextHolder. Real "no token ->
 * 401/403" enforcement is exercised by the Postman collection's security
 * folder against the running application.
 */
@WebMvcTest(controllers = LearningTopicController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LearningTopicControllerTest {

    private static final Long CALLER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.devcommand.devcommand.security.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LearningTopicService learningTopicService;

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

    private LearningTopicResponse sampleResponse() {
        return new LearningTopicResponse(
                10L, "Spring Boot", "Spring Security", 65, LearningStatus.IN_PROGRESS,
                8.5, "https://example.com", "Currently learning JWT authentication",
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void create_returns201WithCreatedTopic() throws Exception {
        CreateLearningTopicRequest request = new CreateLearningTopicRequest(
                "Spring Boot", "Spring Security", 65, LearningStatus.IN_PROGRESS,
                8.5, "https://example.com", "Currently learning JWT authentication"
        );
        when(learningTopicService.create(any(), eq(CALLER_ID))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/learning")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.technology").value("Spring Boot"));
    }

    @Test
    void create_withBlankTechnology_returns400WithFieldError() throws Exception {
        CreateLearningTopicRequest invalidRequest = new CreateLearningTopicRequest(
                "", "Spring Security", 65, LearningStatus.IN_PROGRESS, null, null, null
        );

        mockMvc.perform(post("/api/learning")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.technology").exists());
    }

    @Test
    void create_withProgressOver100_returns400WithFieldError() throws Exception {
        CreateLearningTopicRequest invalidRequest = new CreateLearningTopicRequest(
                "Spring Boot", "Spring Security", 150, LearningStatus.IN_PROGRESS, null, null, null
        );

        mockMvc.perform(post("/api/learning")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.progress").exists());
    }

    @Test
    void create_withNegativeProgress_returns400WithFieldError() throws Exception {
        CreateLearningTopicRequest invalidRequest = new CreateLearningTopicRequest(
                "Spring Boot", "Spring Security", -5, LearningStatus.IN_PROGRESS, null, null, null
        );

        mockMvc.perform(post("/api/learning")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.progress").exists());
    }

    @Test
    void create_withNegativeHoursSpent_returns400WithFieldError() throws Exception {
        CreateLearningTopicRequest invalidRequest = new CreateLearningTopicRequest(
                "Spring Boot", "Spring Security", 65, LearningStatus.IN_PROGRESS, -3.0, null, null
        );

        mockMvc.perform(post("/api/learning")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.hoursSpent").exists());
    }

    @Test
    void getAll_returnsPageForTheAuthenticatedUser() throws Exception {
        Page<LearningTopicResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(learningTopicService.getAll(eq(CALLER_ID), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/learning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void getById_whenServiceThrowsNotFound_propagatesAs404() throws Exception {
        when(learningTopicService.getById(eq(999L), eq(CALLER_ID)))
                .thenThrow(new ResourceNotFoundException("Learning topic not found: 999"));

        mockMvc.perform(get("/api/learning/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/learning/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProgress_returns200WithUpdatedProgress() throws Exception {
        LearningTopicResponse updated = new LearningTopicResponse(
                10L, "Spring Boot", "Spring Security", 90, LearningStatus.IN_PROGRESS,
                8.5, "https://example.com", "notes", LocalDateTime.now(), LocalDateTime.now()
        );
        when(learningTopicService.updateProgress(eq(10L), eq(CALLER_ID), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/learning/10/progress")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProgressUpdateRequest(90))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(90));
    }

    @Test
    void updateProgress_withOutOfRangeValue_returns400() throws Exception {
        mockMvc.perform(patch("/api/learning/10/progress")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ProgressUpdateRequest(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.progress").exists());
    }

    @Test
    void complete_returns200WithCompletedStatusAndFullProgress() throws Exception {
        LearningTopicResponse completed = new LearningTopicResponse(
                10L, "Spring Boot", "Spring Security", 100, LearningStatus.COMPLETED,
                8.5, "https://example.com", "notes", LocalDateTime.now(), LocalDateTime.now()
        );
        when(learningTopicService.complete(eq(10L), eq(CALLER_ID))).thenReturn(completed);

        mockMvc.perform(patch("/api/learning/10/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100));
    }

    @Test
    void completed_returnsListForTheAuthenticatedUser() throws Exception {
        when(learningTopicService.completedTopics(CALLER_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/learning/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void inProgress_returnsListForTheAuthenticatedUser() throws Exception {
        when(learningTopicService.inProgressTopics(CALLER_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/learning/in-progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }
}

