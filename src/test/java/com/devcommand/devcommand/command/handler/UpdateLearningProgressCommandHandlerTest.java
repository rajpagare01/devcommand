package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.learning.dto.UpdateLearningTopicRequest;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.learning.service.LearningTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

        when(learningTopicRepository.findAll(any(Specification.class))).thenReturn(List.of(fakeTopic));

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

        when(learningTopicRepository.findAll(any(Specification.class))).thenReturn(List.of(fakeTopic));

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
        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId, new CommandParameters(Map.of("topic", "Kafka", "progress", 50)));

        when(learningTopicRepository.findAll(any(Specification.class))).thenReturn(List.of());

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("I couldn't find a learning topic matching 'Kafka'.", result.message());
        
        verify(learningTopicService, never()).updateProgress(any(), any(), any());
    }
    
    @Test
    void handle_InvalidProgress_ReturnsFailure() {
        Long userId = 99L;
        Command command = new Command(CommandType.UPDATE_LEARNING_PROGRESS, userId, new CommandParameters(Map.of("topic", "Spring Security", "progress", 105)));

        LearningTopic fakeTopic = new LearningTopic();
        fakeTopic.setId(1L);
        fakeTopic.setTechnology("Java");
        fakeTopic.setTopic("Spring Security");
        fakeTopic.setProgress(10);

        when(learningTopicRepository.findAll(any(Specification.class))).thenReturn(List.of(fakeTopic));

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Progress must be between 0 and 100.", result.message());
    }
}
