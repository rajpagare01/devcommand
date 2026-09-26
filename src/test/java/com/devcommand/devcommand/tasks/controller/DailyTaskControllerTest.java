package com.devcommand.devcommand.tasks.controller;

import com.devcommand.devcommand.exception.GlobalExceptionHandler;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
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
 * Same slice-test approach as DsaProblemControllerTest: addFilters=false
 * skips the real security filter chain, and the caller's identity is pushed
 * directly into SecurityContextHolder so @AuthenticationPrincipal resolves
 * exactly as it would in production. Real "no token -> 401/403" enforcement
 * is exercised by the Postman collection's security folder against the
 * running application, not here.
 */
@WebMvcTest(controllers = DailyTaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DailyTaskControllerTest {

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
    private DailyTaskService dailyTaskService;

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

    private DailyTaskResponse sampleResponse() {
        return new DailyTaskResponse(
                10L, "Revise Spring Security", "Review JWT authentication and filters",
                TaskCategory.LEARNING, TaskPriority.HIGH, DailyTaskStatus.TODO,
                LocalDate.of(2026, 9, 25), null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void create_returns201WithCreatedTask() throws Exception {
        CreateDailyTaskRequest request = new CreateDailyTaskRequest(
                "Revise Spring Security", "Review JWT authentication and filters",
                TaskCategory.LEARNING, TaskPriority.HIGH, DailyTaskStatus.TODO, LocalDate.of(2026, 9, 25)
        );
        when(dailyTaskService.create(any(), eq(CALLER_ID))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Revise Spring Security"));
    }

    @Test
    void create_withBlankTitle_returns400WithFieldError() throws Exception {
        CreateDailyTaskRequest invalidRequest = new CreateDailyTaskRequest(
                "", null, TaskCategory.PERSONAL, TaskPriority.LOW, DailyTaskStatus.TODO, null
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void create_withMissingCategory_returns400WithFieldError() throws Exception {
        String bodyMissingCategory = """
                {
                  "title": "Some task",
                  "priority": "LOW",
                  "status": "TODO"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .contentType("application/json")
                        .content(bodyMissingCategory))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.category").exists());
    }

    @Test
    void getAll_returnsPageForTheAuthenticatedUser() throws Exception {
        Page<DailyTaskResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(dailyTaskService.getAll(eq(CALLER_ID), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void getById_whenServiceThrowsNotFound_propagatesAs404() throws Exception {
        when(dailyTaskService.getById(eq(999L), eq(CALLER_ID)))
                .thenThrow(new ResourceNotFoundException("Task not found: 999"));

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/tasks/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void complete_returns200WithUpdatedStatus() throws Exception {
        DailyTaskResponse completed = new DailyTaskResponse(
                10L, "Revise Spring Security", "desc", TaskCategory.LEARNING, TaskPriority.HIGH,
                DailyTaskStatus.COMPLETED, LocalDate.of(2026, 9, 25), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(dailyTaskService.complete(eq(10L), eq(CALLER_ID))).thenReturn(completed);

        mockMvc.perform(patch("/api/tasks/10/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void start_returns200WithInProgressStatus() throws Exception {
        DailyTaskResponse inProgress = new DailyTaskResponse(
                10L, "Revise Spring Security", "desc", TaskCategory.LEARNING, TaskPriority.HIGH,
                DailyTaskStatus.IN_PROGRESS, LocalDate.of(2026, 9, 25), null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(dailyTaskService.start(eq(10L), eq(CALLER_ID))).thenReturn(inProgress);

        mockMvc.perform(patch("/api/tasks/10/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void today_returnsListForTheAuthenticatedUser() throws Exception {
        when(dailyTaskService.today(CALLER_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/tasks/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void upcoming_returnsListForTheAuthenticatedUser() throws Exception {
        when(dailyTaskService.upcoming(CALLER_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/tasks/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void completed_returnsListForTheAuthenticatedUser() throws Exception {
        when(dailyTaskService.completed(CALLER_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/tasks/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }
}

