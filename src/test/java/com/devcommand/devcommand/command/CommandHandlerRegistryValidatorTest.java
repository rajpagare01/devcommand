package com.devcommand.devcommand.command;

import com.devcommand.devcommand.command.handler.CommandHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommandHandlerRegistryValidator}.
 *
 * These tests exercise the static {@code validate()} method directly — no Spring context needed.
 */
class CommandHandlerRegistryValidatorTest {

    // ------------------------------------------------------------------ Helpers

    /** Creates a minimal stub handler that supports exactly one CommandType. */
    private static CommandHandler stubHandler(CommandType type) {
        return new CommandHandler() {
            @Override
            public boolean supports(CommandType t) {
                return t == type;
            }

            @Override
            public com.devcommand.devcommand.command.CommandResult handle(Command command) {
                return CommandResult.success("stub");
            }

            @Override
            public String toString() {
                return "StubHandler[" + type + "]";
            }
        };
    }

    /**
     * Builds a complete, valid handler list with exactly one handler per
     * non-reserved CommandType.
     */
    private static List<CommandHandler> completeValidRegistry() {
        return Arrays.stream(CommandType.values())
                .filter(t -> !t.isReserved())
                .map(CommandHandlerRegistryValidatorTest::stubHandler)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ Reserved type assertions

    @Test
    void readTasksToday_isMarkedReserved() {
        assertTrue(CommandType.READ_TASKS_TODAY.isReserved(),
                "READ_TASKS_TODAY must be marked reserved (no handler registered yet)");
    }

    @Test
    void confirmAction_isNotReserved() {
        assertFalse(CommandType.CONFIRM_ACTION.isReserved(),
                "CONFIRM_ACTION has a real handler (ConfirmActionCommandHandler) and must NOT be reserved");
    }

    @Test
    void cancelAction_isNotReserved() {
        assertFalse(CommandType.CANCEL_ACTION.isReserved(),
                "CANCEL_ACTION has a real handler (CancelActionCommandHandler) and must NOT be reserved");
    }

    @Test
    void noOtherTypesAreReserved() {
        // Only READ_TASKS_TODAY should be reserved at this point
        List<CommandType> reserved = Arrays.stream(CommandType.values())
                .filter(CommandType::isReserved)
                .collect(Collectors.toList());

        assertEquals(List.of(CommandType.READ_TASKS_TODAY), reserved,
                "Only READ_TASKS_TODAY should be reserved. If you are adding a new reserved type, " +
                "update this test to reflect the intentional decision.");
    }

    // ------------------------------------------------------------------ Valid registry

    @Test
    void validate_completeRegistry_passes() {
        // Should not throw
        assertDoesNotThrow(() -> CommandHandlerRegistryValidator.validate(completeValidRegistry()));
    }

    // ------------------------------------------------------------------ Missing handler

    @Test
    void validate_missingHandler_throwsWithCommandType() {
        // Remove the handler for CREATE_TASK
        List<CommandHandler> incomplete = completeValidRegistry().stream()
                .filter(h -> !h.supports(CommandType.CREATE_TASK))
                .collect(Collectors.toList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(incomplete));

        String msg = ex.getMessage();
        assertTrue(msg.contains("MISSING_HANDLER"), "Error must identify MISSING_HANDLER violation");
        assertTrue(msg.contains("CREATE_TASK"), "Error must name the offending CommandType");
        assertTrue(msg.contains("Expected: 1"), "Error must state expected count");
        assertTrue(msg.contains("Actual: 0"), "Error must state actual count");
    }

    @Test
    void validate_missingHandlerForReadPendingTasks_throwsDescriptiveError() {
        List<CommandHandler> incomplete = completeValidRegistry().stream()
                .filter(h -> !h.supports(CommandType.READ_PENDING_TASKS))
                .collect(Collectors.toList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(incomplete));

        assertTrue(ex.getMessage().contains("READ_PENDING_TASKS"));
    }

    @Test
    void validate_emptyRegistry_throwsForEveryExecutableType() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(new ArrayList<>()));

        String msg = ex.getMessage();
        // Every non-reserved type must be mentioned
        long violations = Arrays.stream(CommandType.values())
                .filter(t -> !t.isReserved())
                .filter(t -> msg.contains(t.name()))
                .count();

        long executableCount = Arrays.stream(CommandType.values())
                .filter(t -> !t.isReserved())
                .count();

        assertEquals(executableCount, violations,
                "Every executable CommandType should appear in the error when the registry is empty");
    }

    // ------------------------------------------------------------------ Duplicate handler

    @Test
    void validate_duplicateHandler_throwsWithHandlerNames() {
        List<CommandHandler> withDuplicate = new ArrayList<>(completeValidRegistry());
        // Add a second handler for DELETE_TASK
        withDuplicate.add(stubHandler(CommandType.DELETE_TASK));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(withDuplicate));

        String msg = ex.getMessage();
        assertTrue(msg.contains("DUPLICATE_HANDLER"), "Error must identify DUPLICATE_HANDLER violation");
        assertTrue(msg.contains("DELETE_TASK"), "Error must name the offending CommandType");
        assertTrue(msg.contains("Expected: 1"), "Error must state expected count");
        assertTrue(msg.contains("Actual: 2"), "Error must state actual count");
    }

    @Test
    void validate_multipleViolations_reportsAllInOneException() {
        // Missing CREATE_TASK + duplicate READ_DSA_STATS
        List<CommandHandler> broken = completeValidRegistry().stream()
                .filter(h -> !h.supports(CommandType.CREATE_TASK))
                .collect(Collectors.toList());
        broken.add(stubHandler(CommandType.READ_DSA_STATS));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(broken));

        String msg = ex.getMessage();
        assertTrue(msg.contains("CREATE_TASK"), "CREATE_TASK missing should be reported");
        assertTrue(msg.contains("READ_DSA_STATS"), "READ_DSA_STATS duplicate should be reported");
    }

    // ------------------------------------------------------------------ Reserved-with-handler violation

    @Test
    void validate_reservedTypeWithHandler_throwsDescriptiveError() {
        List<CommandHandler> withReservedHandler = new ArrayList<>(completeValidRegistry());
        // Accidentally add a handler for the reserved type
        withReservedHandler.add(stubHandler(CommandType.READ_TASKS_TODAY));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(withReservedHandler));

        String msg = ex.getMessage();
        assertTrue(msg.contains("RESERVED_WITH_HANDLER"), "Error must identify RESERVED_WITH_HANDLER violation");
        assertTrue(msg.contains("READ_TASKS_TODAY"), "Error must name the offending reserved type");
    }

    // ------------------------------------------------------------------ READ_TASKS_TODAY specific

    @Test
    void validate_readTasksToday_noHandlerRequired() {
        // A registry with all executable types covered but NOT READ_TASKS_TODAY must pass
        List<CommandHandler> validWithoutToday = completeValidRegistry().stream()
                .filter(h -> !h.supports(CommandType.READ_TASKS_TODAY))
                .collect(Collectors.toList());

        assertDoesNotThrow(() -> CommandHandlerRegistryValidator.validate(validWithoutToday),
                "READ_TASKS_TODAY is reserved; validation must pass without a handler for it");
    }

    // ------------------------------------------------------------------ Confirm/Cancel are mandatory

    @Test
    void validate_missingConfirmActionHandler_fails() {
        List<CommandHandler> missing = completeValidRegistry().stream()
                .filter(h -> !h.supports(CommandType.CONFIRM_ACTION))
                .collect(Collectors.toList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(missing));

        assertTrue(ex.getMessage().contains("CONFIRM_ACTION"),
                "CONFIRM_ACTION must require a handler (it is not reserved)");
    }

    @Test
    void validate_missingCancelActionHandler_fails() {
        List<CommandHandler> missing = completeValidRegistry().stream()
                .filter(h -> !h.supports(CommandType.CANCEL_ACTION))
                .collect(Collectors.toList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> CommandHandlerRegistryValidator.validate(missing));

        assertTrue(ex.getMessage().contains("CANCEL_ACTION"),
                "CANCEL_ACTION must require a handler (it is not reserved)");
    }
}
