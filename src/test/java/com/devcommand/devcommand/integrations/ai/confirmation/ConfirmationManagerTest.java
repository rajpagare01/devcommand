package com.devcommand.devcommand.integrations.ai.confirmation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmationManagerTest {

    private ConfirmationManager confirmationManager;

    @BeforeEach
    void setUp() {
        confirmationManager = new ConfirmationManager();
    }

    @Test
    @DisplayName("Creates and retrieves pending action by token and by active user")
    void testCreateAndRetrieve() {
        Long userId = 101L;
        PendingAction pending = confirmationManager.createPendingAction(
                userId,
                "update_job_application_status",
                Map.of("id", 1, "status", "REJECTED"),
                "Are you sure you want to mark job as REJECTED?"
        );

        assertNotNull(pending.confirmationId());
        assertEquals(userId, pending.userId());
        assertEquals("update_job_application_status", pending.toolName());
        assertFalse(pending.isExpired());

        // Lookup by token
        Optional<PendingAction> byToken = confirmationManager.getPendingAction(pending.confirmationId(), userId);
        assertTrue(byToken.isPresent());
        assertEquals(pending.confirmationId(), byToken.get().confirmationId());

        // Lookup active by userId
        Optional<PendingAction> active = confirmationManager.getActivePendingAction(userId);
        assertTrue(active.isPresent());
        assertEquals(pending.confirmationId(), active.get().confirmationId());
    }

    @Test
    @DisplayName("Consuming active action removes it so it cannot be double-consumed")
    void testConsumeActive() {
        Long userId = 102L;
        PendingAction pending = confirmationManager.createPendingAction(
                userId,
                "create_task",
                Map.of("title", "Important task"),
                "Confirm creation"
        );

        Optional<PendingAction> consumed = confirmationManager.consumeActive(userId);
        assertTrue(consumed.isPresent());
        assertEquals(pending.confirmationId(), consumed.get().confirmationId());

        // Second consume attempt should be empty
        Optional<PendingAction> secondConsume = confirmationManager.consumeActive(userId);
        assertTrue(secondConsume.isEmpty());

        // By-token lookup should also be gone
        Optional<PendingAction> byToken = confirmationManager.getPendingAction(pending.confirmationId(), userId);
        assertTrue(byToken.isEmpty());
    }

    @Test
    @DisplayName("Cancel active pending action removes it successfully")
    void testCancelActive() {
        Long userId = 103L;
        PendingAction pending = confirmationManager.createPendingAction(
                userId,
                "delete_task",
                Map.of("taskId", 55),
                "Confirm deletion"
        );

        boolean cancelled = confirmationManager.cancelActive(userId);
        assertTrue(cancelled);

        // Verification: active is now empty
        assertTrue(confirmationManager.getActivePendingAction(userId).isEmpty());
        assertTrue(confirmationManager.getPendingAction(pending.confirmationId(), userId).isEmpty());
    }

    @Test
    @DisplayName("Action cannot be consumed by a different user")
    void testUserIsolation() {
        Long userA = 104L;
        Long userB = 105L;

        PendingAction pending = confirmationManager.createPendingAction(
                userA,
                "update_task_status",
                Map.of("id", 9),
                "Confirm status update"
        );

        // User B tries to lookup or consume User A's token
        Optional<PendingAction> peekByUserB = confirmationManager.getPendingAction(pending.confirmationId(), userB);
        assertTrue(peekByUserB.isEmpty());

        Optional<PendingAction> consumeByUserB = confirmationManager.consume(pending.confirmationId(), userB);
        assertTrue(consumeByUserB.isEmpty());

        // Still intact for User A
        Optional<PendingAction> peekByUserA = confirmationManager.getPendingAction(pending.confirmationId(), userA);
        assertTrue(peekByUserA.isPresent());
    }
}
