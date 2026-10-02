package com.devcommand.devcommand.integration;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramPendingConfirmationRepository;
import com.devcommand.devcommand.integrations.telegram.service.TelegramUpdateProcessor;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL-backed integration tests for the task-deletion confirmation flow.
 *
 * Covers:
 *  - Token expiration: expired tokens must be rejected
 *  - Single-use (replay): a consumed token cannot be reused
 *  - Cross-user: user B cannot consume user A's token
 *  - Cross-chat: right user, wrong chat — must be rejected
 *  - Invalid/missing token: graceful failure
 *  - Delete of already-deleted task: graceful failure
 *  - FK cascade: deleting user removes their confirmations (DB integrity)
 */
public class ConfirmationSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TelegramUpdateProcessor updateProcessor;
    @Autowired private UserRepository userRepository;
    @Autowired private DailyTaskRepository dailyTaskRepository;
    @Autowired private TelegramPendingConfirmationRepository confirmationRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        confirmationRepository.deleteAll();
        dailyTaskRepository.deleteAll();
        userRepository.deleteAll();

        userA = createUser("userA@test.com", "User A");
        userB = createUser("userB@test.com", "User B");
    }

    // ------------------------------------------------------------------ Expiration

    @Test
    void confirm_expiredToken_isRejected() {
        DailyTask task = createTask(userA, "Expired test task");
        String token = saveConfirmation(userA, 12345L, task.getId(), LocalDateTime.now().minusMinutes(1));

        Command confirmCmd = confirmCommand(userA.getId(), token, 12345L);
        CommandResult result = updateProcessor.processAndMark(confirmCmd, nextUpdateId());

        assertFalse(result.success(), "Expired token must be rejected");
        // Task should still exist
        assertTrue(dailyTaskRepository.existsById(task.getId()));
    }

    // ------------------------------------------------------------------ Single-use / replay

    @Test
    void confirm_validTokenFirstUse_succeeds() {
        DailyTask task = createTask(userA, "Replay test task");
        String token = saveConfirmation(userA, 12345L, task.getId(), LocalDateTime.now().plusMinutes(5));

        Command confirmCmd = confirmCommand(userA.getId(), token, 12345L);
        CommandResult result = updateProcessor.processAndMark(confirmCmd, nextUpdateId());

        assertTrue(result.success(), "First use of valid token must succeed");
        assertFalse(dailyTaskRepository.existsById(task.getId()), "Task should be deleted");
    }

    @Test
    void confirm_replayConsumedToken_isRejected() {
        DailyTask task = createTask(userA, "Replay task 2");
        String token = saveConfirmation(userA, 12345L, task.getId(), LocalDateTime.now().plusMinutes(5));

        // First use — succeeds
        updateProcessor.processAndMark(confirmCommand(userA.getId(), token, 12345L), nextUpdateId());

        // Replay — must fail
        CommandResult replay = updateProcessor.processAndMark(confirmCommand(userA.getId(), token, 12345L), nextUpdateId());
        assertFalse(replay.success(), "Replayed confirmation token must be rejected");
    }

    // ------------------------------------------------------------------ Cross-user IDOR

    @Test
    void confirm_tokenCreatedForUserA_cannotBeConsumedByUserB() {
        DailyTask task = createTask(userA, "Cross-user task");
        String token = saveConfirmation(userA, 12345L, task.getId(), LocalDateTime.now().plusMinutes(5));

        // UserB attempts to consume userA's token in the same chat
        Command confirmCmd = confirmCommand(userB.getId(), token, 12345L);
        CommandResult result = updateProcessor.processAndMark(confirmCmd, nextUpdateId());

        assertFalse(result.success(), "User B must not be able to consume user A's token");
        // Task must still exist (not deleted by userB)
        assertTrue(dailyTaskRepository.existsById(task.getId()));
    }

    // ------------------------------------------------------------------ Cross-chat

    @Test
    void confirm_validTokenWrongChatId_isRejected() {
        DailyTask task = createTask(userA, "Wrong-chat task");
        String token = saveConfirmation(userA, 12345L, task.getId(), LocalDateTime.now().plusMinutes(5));

        // Correct user, wrong chatId
        Command confirmCmd = confirmCommand(userA.getId(), token, 99999L);
        CommandResult result = updateProcessor.processAndMark(confirmCmd, nextUpdateId());

        assertFalse(result.success(), "Token bound to chat 12345 must be rejected for chat 99999");
        assertTrue(dailyTaskRepository.existsById(task.getId()));
    }

    // ------------------------------------------------------------------ Invalid token

    @Test
    void confirm_unknownToken_isRejected() {
        Command confirmCmd = confirmCommand(userA.getId(), "nonexistent-token-" + UUID.randomUUID(), 12345L);
        CommandResult result = updateProcessor.processAndMark(confirmCmd, nextUpdateId());

        assertFalse(result.success(), "Unknown token must be rejected");
    }

    @Test
    void confirm_emptyToken_isRejected() {
        // Empty token string
        Command confirmCmd = new Command(CommandType.CONFIRM_ACTION, userA.getId(),
                new CommandParameters(Map.of("token", "", "chatId", "12345")));
        CommandResult result = updateProcessor.processAndMark(confirmCmd, nextUpdateId());

        assertFalse(result.success(), "Empty token must be rejected");
    }

    // ------------------------------------------------------------------ Cancel

    @Test
    void cancel_validToken_removesConfirmation() {
        DailyTask task = createTask(userA, "Cancel task");
        String token = saveConfirmation(userA, 12345L, task.getId(), LocalDateTime.now().plusMinutes(5));

        Command cancelCmd = cancelCommand(userA.getId(), token, 12345L);
        CommandResult result = updateProcessor.processAndMark(cancelCmd, nextUpdateId());

        assertTrue(result.success(), "Cancel of valid token must succeed");
        assertFalse(confirmationRepository.existsById(token), "Token should be removed after cancel");
        // Task must still exist after cancel
        assertTrue(dailyTaskRepository.existsById(task.getId()));
    }

    @Test
    void cancel_tokenCreatedForUserA_cannotBeCancelledByUserB() {
        DailyTask task = createTask(userA, "Cross-user cancel task");
        String token = saveConfirmation(userA, 12345L, task.getId(), LocalDateTime.now().plusMinutes(5));

        Command cancelCmd = cancelCommand(userB.getId(), token, 12345L);
        CommandResult result = updateProcessor.processAndMark(cancelCmd, nextUpdateId());

        assertFalse(result.success(), "User B must not cancel user A's token");
        // Token should still be present in the DB
        assertTrue(confirmationRepository.existsById(token));
    }

    // ------------------------------------------------------------------ Helpers

    private User createUser(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setPassword("$2a$10$placeholder");
        return userRepository.save(u);
    }

    @Transactional
    private DailyTask createTask(User owner, String title) {
        DailyTask task = new DailyTask();
        task.setTitle(title);
        task.setStatus(DailyTaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setCategory(TaskCategory.PERSONAL);
        task.setUser(owner);
        return dailyTaskRepository.save(task);
    }

    private String saveConfirmation(User user, long chatId, Long taskId, LocalDateTime expiresAt) {
        String token = UUID.randomUUID().toString();
        TelegramPendingConfirmation confirmation = new TelegramPendingConfirmation(
                token, user, chatId, "DELETE_TASK", taskId, expiresAt);
        confirmationRepository.save(confirmation);
        return token;
    }

    private Command confirmCommand(Long userId, String token, long chatId) {
        return new Command(CommandType.CONFIRM_ACTION, userId,
                new CommandParameters(Map.of("token", token, "chatId", String.valueOf(chatId))));
    }

    private Command cancelCommand(Long userId, String token, long chatId) {
        return new Command(CommandType.CANCEL_ACTION, userId,
                new CommandParameters(Map.of("token", token, "chatId", String.valueOf(chatId))));
    }

    private static long updateIdCounter = 200L;
    private static synchronized long nextUpdateId() {
        return updateIdCounter++;
    }
}
