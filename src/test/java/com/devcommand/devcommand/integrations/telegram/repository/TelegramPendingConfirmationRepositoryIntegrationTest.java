package com.devcommand.devcommand.integrations.telegram.repository;

import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.integration.AbstractIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TelegramPendingConfirmationRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TelegramPendingConfirmationRepository confirmationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyTaskRepository dailyTaskRepository;

    @Test
    void testAtomicConsumption() {
        // Prepare test data
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("test_hash");
        User savedUser = userRepository.saveAndFlush(user);

        DailyTask task = new DailyTask();
        task.setTitle("Test Task");
        task.setCategory(TaskCategory.PROJECT);
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(DailyTaskStatus.TODO);
        task.setUser(savedUser);
        DailyTask savedTask = dailyTaskRepository.saveAndFlush(task);

        String token = UUID.randomUUID().toString();
        TelegramPendingConfirmation conf = new TelegramPendingConfirmation(
                token, savedUser, 12345L, "DELETE_TASK", savedTask.getId(), LocalDateTime.now().plusMinutes(5)
        );
        confirmationRepository.saveAndFlush(conf);

        // First consumption should succeed
        int first = confirmationRepository.consumeConfirmation(
                token, savedUser.getId(), 12345L, "DELETE_TASK", LocalDateTime.now());
        assertEquals(1, first, "First consumption should delete exactly one row.");

        // Second consumption should find nothing (row is gone)
        int second = confirmationRepository.consumeConfirmation(
                token, savedUser.getId(), 12345L, "DELETE_TASK", LocalDateTime.now());
        assertEquals(0, second, "Second consumption should find no row.");

        // Row must be gone
        assertTrue(confirmationRepository.findById(token).isEmpty());
    }

    @Test
    void testCancelConfirmation() {
        User newUser = new User();
        newUser.setName("Cancel");
        newUser.setEmail("cancel@example.com");
        newUser.setPassword("hash");
        User user = userRepository.saveAndFlush(newUser);
        DailyTask task = new DailyTask();
        task.setTitle("Cancel Task");
        task.setCategory(TaskCategory.PROJECT);
        task.setPriority(TaskPriority.LOW);
        task.setStatus(DailyTaskStatus.TODO);
        task.setUser(user);
        DailyTask savedTask = dailyTaskRepository.saveAndFlush(task);

        String token = UUID.randomUUID().toString();
        confirmationRepository.saveAndFlush(new TelegramPendingConfirmation(
                token, user, 12345L, "DELETE_TASK", savedTask.getId(), LocalDateTime.now().plusMinutes(5)
        ));

        int cancelled = confirmationRepository.cancelConfirmation(token, user.getId(), 12345L);
        assertEquals(1, cancelled);
        
        Optional<TelegramPendingConfirmation> after = confirmationRepository.findById(token);
        assertTrue(after.isEmpty());
    }
}
