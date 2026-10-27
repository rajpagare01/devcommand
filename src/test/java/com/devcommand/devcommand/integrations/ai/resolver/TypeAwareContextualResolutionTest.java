package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.model.ContextualEntity;
import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.dsa.catalog.service.DsaProblemCatalogService;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.mapper.JobApplicationMapper;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.learning.mapper.LearningTopicMapper;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.projects.entity.Project;
import com.devcommand.devcommand.projects.mapper.ProjectMapper;
import com.devcommand.devcommand.projects.repository.ProjectRepository;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import com.devcommand.devcommand.integrations.ai.resolver.ResolutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TypeAwareContextualResolutionTest {

    @Mock private DsaProblemCatalogService dsaCatalogService;
    @Mock private DailyTaskService dailyTaskService;
    @Mock private ConversationContextService contextService;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMapper projectMapper;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private JobApplicationMapper jobApplicationMapper;
    @Mock private LearningTopicRepository learningTopicRepository;
    @Mock private LearningTopicMapper learningTopicMapper;

    @InjectMocks
    private DefaultDomainEntityResolver resolver;

    private final Long userId = 42L;
    private final String conversationId = "conv-multi-entity";

    @Test
    @DisplayName("Critical Acceptance: Type-aware context selection resolves 'it' to Task for task operation and to DSA for DSA operation")
    void testTypeAwareContextResolution() {
        // Given: Context contains BOTH a DSA problem and a Task
        ContextualEntity dsaEntity = ContextualEntity.dsa(101L, "Two Sum", "LEETCODE", conversationId, Map.of("difficulty", "EASY", "topic", "Arrays & Hashing"));
        ContextualEntity taskEntity = ContextualEntity.task(202L, "Revise Two Sum", conversationId, Map.of("priority", "HIGH", "status", "TODO"));

        when(contextService.getEntity(userId, conversationId, ContextType.LAST_DSA_PROBLEM))
                .thenReturn(Optional.of(dsaEntity));
        when(contextService.getEntity(userId, conversationId, ContextType.LAST_TASK))
                .thenReturn(Optional.of(taskEntity));

        DailyTaskResponse taskResponse = new DailyTaskResponse(
                202L, "Revise Two Sum", null, TaskCategory.PERSONAL, TaskPriority.HIGH, DailyTaskStatus.TODO, null, null, null, null
        );
        when(dailyTaskService.getById(202L, userId)).thenReturn(taskResponse);

        // 1. Task operation query ("Make it high priority" -> resolveTask("it"))
        TaskResolution taskResolution = resolver.resolveTask(userId, "it", conversationId);
        assertThat(taskResolution.isResolved()).isTrue();
        assertThat(taskResolution.taskId()).isEqualTo(202L);
        assertThat(taskResolution.title()).isEqualTo("Revise Two Sum");

        // 2. DSA operation query ("What difficulty was it?" -> resolveDsaProblem("it"))
        DsaProblemResolution dsaResolution = resolver.resolveDsaProblem(userId, "it", null, null, conversationId);
        assertThat(dsaResolution.isResolved()).isTrue();
        assertThat(dsaResolution.title()).isEqualTo("Two Sum");
        assertThat(dsaResolution.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(dsaResolution.topic()).isEqualTo("Arrays & Hashing");
    }

    @Test
    @DisplayName("Revision task creation: 'Revise it' resolves 'it' to LAST_DSA_PROBLEM")
    void testRevisionTaskResolution() {
        ContextualEntity dsaEntity = ContextualEntity.dsa(101L, "Two Sum", "LEETCODE", conversationId, Map.of("difficulty", "EASY"));
        when(contextService.getEntity(userId, conversationId, ContextType.LAST_DSA_PROBLEM))
                .thenReturn(Optional.of(dsaEntity));

        TaskResolution resolution = resolver.resolveTask(userId, "Revise it", conversationId);

        assertThat(resolution.isResolved()).isTrue();
        assertThat(resolution.title()).isEqualTo("Revise Two Sum");
    }

    @Test
    @DisplayName("Project reference: 'its' or 'that project' resolves to LAST_PROJECT")
    void testProjectResolution() {
        ContextualEntity projectEntity = ContextualEntity.project(55L, "DevCommand", conversationId, Map.of());
        when(contextService.getEntity(userId, conversationId, ContextType.LAST_PROJECT))
                .thenReturn(Optional.of(projectEntity));

        Project mockProject = new Project();
        mockProject.setId(55L);
        mockProject.setName("DevCommand");
        when(projectRepository.findByIdAndUserId(55L, userId)).thenReturn(Optional.of(mockProject));

        ProjectResolution resolution = resolver.resolveProject(userId, "its", conversationId);

        assertThat(resolution.isResolved()).isTrue();
        assertThat(resolution.projectId()).isEqualTo(55L);
        assertThat(resolution.title()).isEqualTo("DevCommand");
    }

    @Test
    @DisplayName("Job application reference: 'the application' resolves to LAST_JOB_APPLICATION")
    void testJobApplicationResolution() {
        ContextualEntity jobEntity = ContextualEntity.job(77L, "Stripe", conversationId, Map.of("role", "Backend Engineer"));
        when(contextService.getEntity(userId, conversationId, ContextType.LAST_JOB_APPLICATION))
                .thenReturn(Optional.of(jobEntity));

        JobApplication mockApp = new JobApplication();
        mockApp.setId(77L);
        mockApp.setCompany("Stripe");
        when(jobApplicationRepository.findByIdAndUserId(77L, userId)).thenReturn(Optional.of(mockApp));

        JobApplicationResolution resolution = resolver.resolveJobApplication(userId, "the application", conversationId);

        assertThat(resolution.isResolved()).isTrue();
        assertThat(resolution.company()).isEqualTo("Stripe");
    }

    @Test
    @DisplayName("Context miss: returns ambiguous / clarification when context is absent")
    void testContextMissReturnsClarification() {
        when(contextService.getEntity(userId, conversationId, ContextType.LAST_DSA_PROBLEM))
                .thenReturn(Optional.empty());

        DsaProblemResolution resolution = resolver.resolveDsaProblem(userId, "it", null, null, conversationId);

        assertThat(resolution.isResolved()).isFalse();
        assertThat(resolution.status()).isEqualTo(ResolutionStatus.AMBIGUOUS);
        assertThat(resolution.clarificationMessage()).contains("Which DSA problem are you referring to?");
    }
}
