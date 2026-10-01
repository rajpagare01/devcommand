package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.learning.dto.LearningSummary;
import com.devcommand.devcommand.learning.dto.LearningTopicResponse;
import com.devcommand.devcommand.learning.service.LearningTopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadLearningProgressCommandHandlerTest {

    @Mock
    private LearningTopicService learningTopicService;

    private ReadLearningProgressCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReadLearningProgressCommandHandler(learningTopicService);
    }

    @Test
    void supports_ReturnsTrueForReadLearningProgress() {
        assertTrue(handler.supports(CommandType.READ_LEARNING_PROGRESS));
    }

    @Test
    void handle_ReturnsProgressSuccessfully() {
        Long userId = 99L;
        Command command = new Command(CommandType.READ_LEARNING_PROGRESS, userId, new CommandParameters(Collections.emptyMap()));

        LearningSummary summary = new LearningSummary(5L, 2L, 3L, 50.0, 10.5);
        when(learningTopicService.getSummary(eq(userId))).thenReturn(summary);
        
        LearningTopicResponse topic = mock(LearningTopicResponse.class);
        when(topic.topic()).thenReturn("Spring");
        when(topic.progress()).thenReturn(50);
        when(learningTopicService.inProgressTopics(eq(userId))).thenReturn(List.of(topic));

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertTrue(result.message().contains("Total topics: 5"));
        assertTrue(result.message().contains("Completed: 2"));
        assertTrue(result.message().contains("Average progress: 10.5%"), "Actual message: " + result.message());
        assertTrue(result.message().contains("- Spring (50%)"));

        verify(learningTopicService).getSummary(eq(userId));
        verify(learningTopicService).inProgressTopics(eq(userId));
    }
}
