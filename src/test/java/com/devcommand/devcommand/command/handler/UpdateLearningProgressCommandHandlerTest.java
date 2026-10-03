package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.learning.service.LearningTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Updated to stub {@code findByUserIdAndTopicOrTechnologyIgnoreCase}
 * (the DB-scoped query) instead of the removed in-memory {@code findAll(Specification)}.
 */
@ExtendWith(MockitoExtension.class)
class UpdateLearningProgressCommandHandlerTest {

    @Mock
    private LearningTopicService learningTopicService;

    @Mock
    private LearningTopicRepository learningTopicRepository;

    private UpdateLearningProgressCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateLearningProgressCommandHandler(learningTopicService, learningTopicRepository);
    }

    @Test
    void supports_ReturnsTrueForUpdateLearningProgress() {
        assertTrue(handler.supports(CommandType.UPDATE_LEARNING_PROGRESS));
    }

    @Test
    void handle_UpdatesProgressCorrectly() {
        Long userId = 99L;
        Map<String, Object> params = new HashMap<>();
        params.put("topic", "Spring Security");
        params.put("progress", 50);

        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId, new CommandParameters(params));

        LearningTopic fakeTopic = new LearningTopic();
        fakeTopic.setId(1L);
        fakeTopic.setTechnology("Java");
        fakeTopic.setTopic("Spring Security");
        fakeTopic.setProgress(10);
        fakeTopic.setStatus(LearningStatus.IN_PROGRESS);

        when(learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, "Spring Security"))
                .thenReturn(List.of(fakeTopic));

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertEquals("Updated progress for 'Spring Security' to 50%.", result.message());
        verify(learningTopicService).updateProgress(eq(1L), eq(userId), any());
    }

    @Test
    void handle_UpdatesHoursCorrectly() {
        Long userId = 99L;
        Map<String, Object> params = new HashMap<>();
        params.put("topic", "Spring Security");
        params.put("hours", 2);

        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId, new CommandParameters(params));

        LearningTopic fakeTopic = new LearningTopic();
        fakeTopic.setId(1L);
        fakeTopic.setTechnology("Java");
        fakeTopic.setTopic("Spring Security");
        fakeTopic.setProgress(10);
        fakeTopic.setStatus(LearningStatus.IN_PROGRESS);
        fakeTopic.setHoursSpent(1.5);

        when(learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, "Spring Security"))
                .thenReturn(List.of(fakeTopic));

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertTrue(result.message().contains("Total spent on 'Spring Security' is now 3.5 hours."));

        ArgumentCaptor<UpdateLearningTopicRequest> captor = ArgumentCaptor.forClass(UpdateLearningTopicRequest.class);
        verify(learningTopicService).update(eq(1L), eq(userId), captor.capture());
        assertEquals(3.5, captor.getValue().hoursSpent());
    }

    @Test
    void handle_TopicNotFound_ReturnsFailure() {
        Long userId = 99L;
        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId,
                new CommandParameters(Map.of("topic", "Kafka", "progress", 50)));

        when(learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, "Kafka"))
                .thenReturn(List.of());

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("I couldn't find a learning topic matching 'Kafka'.", result.message());
        verify(learningTopicService, never()).updateProgress(any(), any(), any());
    }

    @Test
    void handle_InvalidProgress_ReturnsFailure() {
        Long userId = 99L;
        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId,
                new CommandParameters(Map.of("topic", "Spring Security", "progress", 105)));

        LearningTopic fakeTopic = new LearningTopic();
        fakeTopic.setId(1L);
        fakeTopic.setTechnology("Java");
        fakeTopic.setTopic("Spring Security");
        fakeTopic.setProgress(10);
        fakeTopic.setStatus(LearningStatus.IN_PROGRESS);

        when(learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, "Spring Security"))
                .thenReturn(List.of(fakeTopic));

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Progress must be between 0 and 100.", result.message());
    }

    @Test
    void handle_NegativeHours_ReturnsFailure() {
        Long userId = 99L;
        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId,
                new CommandParameters(Map.of("topic", "Spring Security", "hours", -1)));

        LearningTopic fakeTopic = new LearningTopic();
        fakeTopic.setId(1L);
        fakeTopic.setTechnology("Java");
        fakeTopic.setTopic("Spring Security");
        fakeTopic.setProgress(10);
        fakeTopic.setStatus(LearningStatus.IN_PROGRESS);

        when(learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, "Spring Security"))
                .thenReturn(List.of(fakeTopic));

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Hours spent cannot be negative.", result.message());
    }

    @Test
    void handle_UsesFirstMatchWhenMultipleResults() {
        Long userId = 99L;
        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId,
                new CommandParameters(Map.of("topic", "spring", "progress", 80)));

        LearningTopic topic1 = new LearningTopic();
        topic1.setId(10L);
        topic1.setTechnology("Spring Boot");
        topic1.setTopic("spring");
        topic1.setProgress(0);
        topic1.setStatus(LearningStatus.NOT_STARTED);

        LearningTopic topic2 = new LearningTopic();
        topic2.setId(20L);
        topic2.setTechnology("Spring MVC");
        topic2.setTopic("spring");
        topic2.setProgress(0);
        topic2.setStatus(LearningStatus.NOT_STARTED);

        when(learningTopicRepository.findByUserIdAndTopicOrTechnologyIgnoreCase(userId, "spring"))
                .thenReturn(List.of(topic1, topic2));

        handler.handle(command);

        // Only the first match (topic1, id=10) should be updated
        verify(learningTopicService).updateProgress(eq(10L), eq(userId), any());
        verify(learningTopicService, never()).updateProgress(eq(20L), any(), any());
    }
}
