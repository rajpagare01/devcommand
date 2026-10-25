package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.conversation.entity.ConversationContext;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.dsa.catalog.entity.DsaProblemCatalogEntry;
import com.devcommand.devcommand.dsa.catalog.service.DsaProblemCatalogService;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.jobs.mapper.JobApplicationMapper;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.learning.mapper.LearningTopicMapper;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.projects.mapper.ProjectMapper;
import com.devcommand.devcommand.projects.repository.ProjectRepository;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainEntityResolverTest {

    @Mock
    private DsaProblemCatalogService dsaCatalogService;

    @Mock
    private DailyTaskService dailyTaskService;

    @Mock
    private ConversationContextService contextService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private JobApplicationMapper jobApplicationMapper;

    @Mock
    private LearningTopicRepository learningTopicRepository;

    @Mock
    private LearningTopicMapper learningTopicMapper;

    private DefaultDomainEntityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DefaultDomainEntityResolver(
                dsaCatalogService,
                dailyTaskService,
                contextService,
                projectRepository,
                projectMapper,
                jobApplicationRepository,
                jobApplicationMapper,
                learningTopicRepository,
                learningTopicMapper
        );
    }

    @Test
    @DisplayName("1. 'Today I solved Two Sum on LeetCode' -> resolves to EASY, LEETCODE, Arrays & Hashing")
    void resolveDsaProblem_twoSum_resolvesAuthoritativeMetadata() {
        DsaProblemCatalogEntry twoSum = DsaProblemCatalogEntry.builder()
                .title("Two Sum")
                .normalizedTitle("twosum")
                .platform("LEETCODE")
                .difficulty(Difficulty.EASY)
                .topic("Arrays & Hashing")
                .problemUrl("https://leetcode.com/problems/two-sum/")
                .build();

        when(dsaCatalogService.findExactMatch("Two Sum")).thenReturn(Optional.of(twoSum));

        DsaProblemResolution res = resolver.resolveDsaProblem("Two Sum", "LEETCODE", null);

        assertTrue(res.isResolved());
        assertEquals(ResolutionStatus.RESOLVED, res.status());
        assertEquals("Two Sum", res.title());
        assertEquals("LEETCODE", res.platform());
        assertEquals(Difficulty.EASY, res.difficulty());
        assertEquals("Arrays & Hashing", res.topic());
        assertEquals("https://leetcode.com/problems/two-sum/", res.problemUrl());
    }

    @Test
    @DisplayName("2. 'I solved Binary Search today' -> resolves to EASY, LEETCODE, Binary Search")
    void resolveDsaProblem_binarySearch_resolvesAuthoritativeMetadata() {
        DsaProblemCatalogEntry binarySearch = DsaProblemCatalogEntry.builder()
                .title("Binary Search")
                .normalizedTitle("binarysearch")
                .platform("LEETCODE")
                .difficulty(Difficulty.EASY)
                .topic("Binary Search")
                .problemUrl("https://leetcode.com/problems/binary-search/")
                .build();

        when(dsaCatalogService.findExactMatch("Binary Search")).thenReturn(Optional.of(binarySearch));

        DsaProblemResolution res = resolver.resolveDsaProblem("Binary Search", null, null);

        assertTrue(res.isResolved());
        assertEquals("Binary Search", res.title());
        assertEquals("LEETCODE", res.platform());
        assertEquals(Difficulty.EASY, res.difficulty());
        assertEquals("Binary Search", res.topic());
    }

    @Test
    @DisplayName("2b. 'I finished 3Sum today' -> resolves to MEDIUM, LEETCODE, Two Pointers")
    void resolveDsaProblem_3Sum_resolvesMediumDifficulty() {
        DsaProblemCatalogEntry threeSum = DsaProblemCatalogEntry.builder()
                .title("3Sum")
                .normalizedTitle("3sum")
                .platform("LEETCODE")
                .difficulty(Difficulty.MEDIUM)
                .topic("Two Pointers")
                .problemUrl("https://leetcode.com/problems/3sum/")
                .build();

        when(dsaCatalogService.findExactMatch("3Sum")).thenReturn(Optional.of(threeSum));

        DsaProblemResolution res = resolver.resolveDsaProblem("3Sum", null, null);

        assertTrue(res.isResolved());
        assertEquals("3Sum", res.title());
        assertEquals("LEETCODE", res.platform());
        assertEquals(Difficulty.MEDIUM, res.difficulty());
        assertEquals("Two Pointers", res.topic());
    }

    @Test
    @DisplayName("3. Contextual pronoun 'Make it high priority' -> resolves to LAST_TASK")
    void resolveTask_contextualPronoun_resolvesToLastTask() {
        Long userId = 100L;
        String chatId = "chat-1";

        ConversationContext context = ConversationContext.builder()
                .contextType(ContextType.LAST_TASK)
                .contextData(Map.of("id", 42L, "title", "Finish Telegram integration"))
                .build();

        when(contextService.getContext(userId, chatId)).thenReturn(Optional.of(context));

        DailyTaskResponse taskResponse = new DailyTaskResponse(
                42L, "Finish Telegram integration", "description",
                TaskCategory.PROJECT, TaskPriority.HIGH, DailyTaskStatus.IN_PROGRESS,
                LocalDate.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(dailyTaskService.getById(42L, userId)).thenReturn(taskResponse);

        TaskResolution res = resolver.resolveTask(userId, "it", chatId);

        assertTrue(res.isResolved());
        assertEquals(42L, res.taskId());
        assertEquals("Finish Telegram integration", res.title());
    }

    @Test
    @DisplayName("4. Ambiguous problem name with multiple candidates -> asks for clarification")
    void resolveDsaProblem_ambiguousName_requestsClarification() {
        when(dsaCatalogService.findExactMatch("Two Sum Special")).thenReturn(Optional.empty());

        DsaProblemCatalogEntry opt1 = DsaProblemCatalogEntry.builder()
                .title("Two Sum II - Input Array Is Sorted")
                .normalizedTitle("twosumiiinputarrayissorted")
                .platform("LEETCODE")
                .difficulty(Difficulty.MEDIUM)
                .topic("Two Pointers")
                .build();

        DsaProblemCatalogEntry opt2 = DsaProblemCatalogEntry.builder()
                .title("Two Sum IV - Input is a BST")
                .normalizedTitle("twosumivinputisabst")
                .platform("LEETCODE")
                .difficulty(Difficulty.EASY)
                .topic("Trees")
                .build();

        when(dsaCatalogService.searchSimilar("Two Sum Special")).thenReturn(List.of(opt1, opt2));

        DsaProblemResolution res = resolver.resolveDsaProblem("Two Sum Special", null, null);

        assertEquals(ResolutionStatus.AMBIGUOUS, res.status());
        assertFalse(res.isResolved());
        assertNotNull(res.clarificationMessage());
        assertTrue(res.clarificationMessage().contains("multiple possible matches"));
    }

    @Test
    @DisplayName("5. Explicit user difficulty is preserved over catalog default")
    void resolveDsaProblem_explicitDifficulty_preservesUserOverride() {
        DsaProblemCatalogEntry twoSum = DsaProblemCatalogEntry.builder()
                .title("Two Sum")
                .normalizedTitle("twosum")
                .platform("LEETCODE")
                .difficulty(Difficulty.EASY)
                .topic("Arrays & Hashing")
                .build();

        when(dsaCatalogService.findExactMatch("Two Sum")).thenReturn(Optional.of(twoSum));

        // User explicitly said "mark it as medium"
        DsaProblemResolution res = resolver.resolveDsaProblem("Two Sum", "LEETCODE", Difficulty.MEDIUM);

        assertTrue(res.isResolved());
        assertEquals(Difficulty.MEDIUM, res.difficulty());
    }

    @Test
    @DisplayName("6. Unknown problem without difficulty -> asks for difficulty clarification")
    void resolveDsaProblem_unknownProblemWithoutDifficulty_asksClarification() {
        when(dsaCatalogService.findExactMatch("Custom Company Problem")).thenReturn(Optional.empty());
        when(dsaCatalogService.searchSimilar("Custom Company Problem")).thenReturn(List.of());

        DsaProblemResolution res = resolver.resolveDsaProblem("Custom Company Problem", "LEETCODE", null);

        assertEquals(ResolutionStatus.UNKNOWN, res.status());
        assertFalse(res.isResolved());
        assertNotNull(res.clarificationMessage());
        assertTrue(res.clarificationMessage().contains("What difficulty level"));
    }

    @Test
    @DisplayName("7. Cross-user entity lookup -> ownership isolation enforced")
    void resolveTask_crossUserAccess_failsSafely() {
        Long attackerUserId = 200L;
        Long victimTaskId = 999L;

        when(dailyTaskService.getById(victimTaskId, attackerUserId))
                .thenThrow(new ResourceNotFoundException("Task not found with id: " + victimTaskId));

        TaskResolution res = resolver.resolveTask(attackerUserId, "999", "chat-1");

        assertFalse(res.isResolved());
        assertEquals(ResolutionStatus.NOT_FOUND, res.status());
    }

    @Test
    @DisplayName("8. Null userId in entity resolver -> rejected immediately")
    void resolveTask_nullUserId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                resolver.resolveTask(null, "some task", "chat-1")
        );
    }
}
