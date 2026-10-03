package com.devcommand.devcommand.dto;

import com.devcommand.devcommand.auth.dto.RegisterRequest;
import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.jobs.dto.CreateInterviewRoundRequest;
import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.InterviewStatus;
import com.devcommand.devcommand.learning.dto.CreateLearningTopicRequest;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectTaskRequest;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for REST DTO Bean Validation constraints.
 *
 * Uses the raw Validator API — no Spring context needed, very fast.
 */
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private <T> Set<ConstraintViolation<T>> validate(T obj) {
        return validator.validate(obj);
    }

    private String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    // ==================================================================
    // RegisterRequest
    // ==================================================================

    @Test
    void registerRequest_valid_noViolations() {
        var req = new RegisterRequest("Alice", "alice@example.com", "Password1!");
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void registerRequest_nameTooLong_fails() {
        var req = new RegisterRequest(repeat('A', 256), "a@b.com", "Password1!");
        assertThat(validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void registerRequest_emailTooLong_fails() {
        String longEmail = repeat('a', 250) + "@b.com";
        var req = new RegisterRequest("Alice", longEmail, "Password1!");
        assertThat(validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void registerRequest_passwordTooLong_fails() {
        var req = new RegisterRequest("Alice", "a@b.com", repeat('x', 101));
        assertThat(validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void registerRequest_passwordTooShort_fails() {
        var req = new RegisterRequest("Alice", "a@b.com", "short");
        assertThat(validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void registerRequest_invalidEmail_fails() {
        var req = new RegisterRequest("Alice", "not-an-email", "Password1!");
        assertThat(validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    // ==================================================================
    // CreateDailyTaskRequest
    // ==================================================================

    @Test
    void createDailyTask_valid_noViolations() {
        var req = new CreateDailyTaskRequest("My task", null, TaskCategory.DSA, TaskPriority.HIGH, DailyTaskStatus.TODO, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void createDailyTask_titleTooLong_fails() {
        var req = new CreateDailyTaskRequest(repeat('X', 256), null, TaskCategory.DSA, TaskPriority.HIGH, DailyTaskStatus.TODO, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void createDailyTask_descriptionTooLong_fails() {
        var req = new CreateDailyTaskRequest("Title", repeat('x', 2001), TaskCategory.DSA, TaskPriority.HIGH, DailyTaskStatus.TODO, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void createDailyTask_titleAtLimit_passes() {
        var req = new CreateDailyTaskRequest(repeat('X', 255), null, TaskCategory.DSA, TaskPriority.HIGH, DailyTaskStatus.TODO, null);
        assertThat(validate(req)).isEmpty();
    }

    // ==================================================================
    // CreateDsaProblemRequest
    // ==================================================================

    @Test
    void createDsaProblem_valid_noViolations() {
        var req = new CreateDsaProblemRequest("Two Sum", "LeetCode", null, "Arrays",
                Difficulty.EASY, ProblemStatus.SOLVED, null, null, null, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void createDsaProblem_titleTooLong_fails() {
        var req = new CreateDsaProblemRequest(repeat('T', 256), "LC", null, "Arrays",
                Difficulty.EASY, ProblemStatus.SOLVED, null, null, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void createDsaProblem_problemUrlTooLong_fails() {
        var req = new CreateDsaProblemRequest("Title", "LC", repeat('u', 2049), "Arrays",
                Difficulty.EASY, ProblemStatus.SOLVED, null, null, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("problemUrl"));
    }

    @Test
    void createDsaProblem_notesTooLong_fails() {
        var req = new CreateDsaProblemRequest("Title", "LC", null, "Arrays",
                Difficulty.EASY, ProblemStatus.SOLVED, null, null, repeat('n', 2001), null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("notes"));
    }

    @Test
    void createDsaProblem_negativeTimeTaken_fails() {
        var req = new CreateDsaProblemRequest("Title", "LC", null, "Arrays",
                Difficulty.EASY, ProblemStatus.SOLVED, null, -1, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("timeTaken"));
    }

    // ==================================================================
    // CreateJobApplicationRequest
    // ==================================================================

    @Test
    void createJobApplication_valid_noViolations() {
        var req = new CreateJobApplicationRequest("Acme", "Engineer", null, null, null, null,
                LocalDate.now(), ApplicationStatus.APPLIED, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void createJobApplication_companyTooLong_fails() {
        var req = new CreateJobApplicationRequest(repeat('C', 256), "Eng", null, null, null, null,
                LocalDate.now(), ApplicationStatus.APPLIED, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("company"));
    }

    @Test
    void createJobApplication_jobUrlTooLong_fails() {
        var req = new CreateJobApplicationRequest("Acme", "Eng", null, repeat('u', 2049), null, null,
                LocalDate.now(), ApplicationStatus.APPLIED, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("jobUrl"));
    }

    @Test
    void createJobApplication_notesTooLong_fails() {
        var req = new CreateJobApplicationRequest("Acme", "Eng", null, null, null, null,
                LocalDate.now(), ApplicationStatus.APPLIED, repeat('n', 2001));
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("notes"));
    }

    // ==================================================================
    // CreateInterviewRoundRequest
    // ==================================================================

    @Test
    void createInterviewRound_valid_noViolations() {
        var req = new CreateInterviewRoundRequest(1, "Technical", null, InterviewStatus.SCHEDULED, null, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void createInterviewRound_roundTypeTooLong_fails() {
        var req = new CreateInterviewRoundRequest(1, repeat('T', 256), null, InterviewStatus.SCHEDULED, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("roundType"));
    }

    @Test
    void createInterviewRound_feedbackTooLong_fails() {
        var req = new CreateInterviewRoundRequest(1, "Tech", null, InterviewStatus.SCHEDULED, repeat('f', 2001), null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("feedback"));
    }

    @Test
    void createInterviewRound_negativeRoundNumber_fails() {
        var req = new CreateInterviewRoundRequest(-1, "Tech", null, InterviewStatus.SCHEDULED, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("roundNumber"));
    }

    // ==================================================================
    // CreateLearningTopicRequest
    // ==================================================================

    @Test
    void createLearningTopic_valid_noViolations() {
        var req = new CreateLearningTopicRequest("Java", "Streams", 50, LearningStatus.IN_PROGRESS, 0.0, null, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void createLearningTopic_technologyTooLong_fails() {
        var req = new CreateLearningTopicRequest(repeat('T', 256), "Topic", 50, LearningStatus.IN_PROGRESS, 0.0, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("technology"));
    }

    @Test
    void createLearningTopic_progressAbove100_fails() {
        var req = new CreateLearningTopicRequest("Java", "Streams", 101, LearningStatus.IN_PROGRESS, 0.0, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("progress"));
    }

    @Test
    void createLearningTopic_progressBelow0_fails() {
        var req = new CreateLearningTopicRequest("Java", "Streams", -1, LearningStatus.IN_PROGRESS, 0.0, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("progress"));
    }

    @Test
    void createLearningTopic_negativeHours_fails() {
        var req = new CreateLearningTopicRequest("Java", "Streams", 50, LearningStatus.IN_PROGRESS, -1.0, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("hoursSpent"));
    }

    @Test
    void createLearningTopic_resourceUrlTooLong_fails() {
        var req = new CreateLearningTopicRequest("Java", "Streams", 50, LearningStatus.IN_PROGRESS, 0.0, repeat('u', 2049), null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("resourceUrl"));
    }

    // ==================================================================
    // ProjectRequest
    // ==================================================================

    @Test
    void projectRequest_valid_noViolations() {
        var req = new ProjectRequest("My Project", null, null, null, ProjectStatus.IN_PROGRESS, null, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void projectRequest_nameTooLong_fails() {
        var req = new ProjectRequest(repeat('P', 256), null, null, null, ProjectStatus.IN_PROGRESS, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void projectRequest_descriptionTooLong_fails() {
        var req = new ProjectRequest("Name", repeat('d', 2001), null, null, ProjectStatus.IN_PROGRESS, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }

    @Test
    void projectRequest_githubUrlTooLong_fails() {
        var req = new ProjectRequest("Name", null, repeat('u', 2049), null, ProjectStatus.IN_PROGRESS, null, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("githubUrl"));
    }

    // ==================================================================
    // ProjectTaskRequest
    // ==================================================================

    @Test
    void projectTaskRequest_valid_noViolations() {
        var req = new ProjectTaskRequest("Task", null, ProjectTaskStatus.TODO, ProjectTaskPriority.MEDIUM, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void projectTaskRequest_titleTooLong_fails() {
        var req = new ProjectTaskRequest(repeat('T', 256), null, ProjectTaskStatus.TODO, ProjectTaskPriority.MEDIUM, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void projectTaskRequest_descriptionTooLong_fails() {
        var req = new ProjectTaskRequest("Title", repeat('d', 2001), ProjectTaskStatus.TODO, ProjectTaskPriority.MEDIUM, null);
        assertThat(validate(req)).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }
}
