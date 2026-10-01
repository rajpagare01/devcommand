package com.devcommand.devcommand.integration;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.integrations.telegram.service.TelegramUpdateProcessor;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.learning.entity.LearningTopic;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Phase2CommandIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TelegramUpdateProcessor updateProcessor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DsaProblemRepository dsaProblemRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private LearningTopicRepository learningTopicRepository;

    private User testUser;

    @BeforeEach
    void setup() {
        dsaProblemRepository.deleteAll();
        jobApplicationRepository.deleteAll();
        learningTopicRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("test@devcommand.com");
        testUser = userRepository.save(user);
    }

    @Test
    void testCreateDsaProblemAndReadStats() {
        // Create DSA Problem
        Command createCmd = new Command(CommandType.CREATE_DSA_PROBLEM, testUser.getId(), new CommandParameters(Map.of(
                "title", "Two Sum", "platform", "LeetCode", "topic", "Arrays", "difficulty", "EASY", "status", "SOLVED", "timeTaken", 10
        )));
        
        CommandResult result1 = updateProcessor.processAndMark(createCmd, 100L);
        assertTrue(result1.success());

        // Verify isolation / stats
        Command readCmd = new Command(CommandType.READ_DSA_STATS, testUser.getId(), new CommandParameters(Map.of()));
        CommandResult result2 = updateProcessor.processAndMark(readCmd, 101L);
        
        assertTrue(result2.success());
        assertTrue(result2.message().contains("Total Solved: 1"));
        assertTrue(result2.message().contains("Easy: 1"));
        
        // Ensure duplicate update processing handles correctly (mocking what happens in TelegramBotService)
        // updateProcessor will just insert into processed_updates.
        // The duplicate logic in TelegramBotService prevents it from reaching the processor, so we don't strictly test the bot here.
    }

    @Test
    void testJobApplicationPipeline() {
        Command createCmd = new Command(CommandType.CREATE_JOB_APPLICATION, testUser.getId(), new CommandParameters(Map.of(
                "company", "Google", "role", "Backend Engineer"
        )));
        updateProcessor.processAndMark(createCmd, 102L);

        Command readCmd = new Command(CommandType.READ_JOB_PIPELINE, testUser.getId(), new CommandParameters(Map.of()));
        CommandResult result = updateProcessor.processAndMark(readCmd, 103L);
        
        assertTrue(result.success());
        assertTrue(result.message().contains("Total Applications: 1"));
        assertTrue(result.message().contains("Applied: 1"));
    }

    @Test
    void testLearningProgressUpdates() {
        // Create it directly to simulate existing topic
        LearningTopic topic = new LearningTopic();
        topic.setTopic("Spring Boot");
        topic.setTechnology("Java");
        topic.setProgress(0);
        topic.setStatus(com.devcommand.devcommand.learning.entity.LearningStatus.IN_PROGRESS);
        topic.setUser(testUser);
        learningTopicRepository.save(topic);

        // Update progress
        Command updateCmd = new Command(CommandType.UPDATE_LEARNING_PROGRESS, testUser.getId(), new CommandParameters(Map.of(
                "topic", "Spring Boot", "progress", 50, "hours", 2
        )));
        CommandResult updateResult = updateProcessor.processAndMark(updateCmd, 104L);
        assertTrue(updateResult.success());
        
        // Read progress
        Command readCmd = new Command(CommandType.READ_LEARNING_PROGRESS, testUser.getId(), new CommandParameters(Map.of()));
        CommandResult readResult = updateProcessor.processAndMark(readCmd, 105L);
        
        assertTrue(readResult.success());
        assertTrue(readResult.message().contains("Total Topics: 1"));
        assertTrue(readResult.message().contains("Total Hours Spent: 2.0"));
    }
}
