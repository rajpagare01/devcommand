package com.devcommand.devcommand.command;

import com.devcommand.devcommand.command.handler.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates the CommandHandler registry at application startup.
 *
 * <p>Rules enforced:
 * <ul>
 *   <li>Every non-reserved {@link CommandType} must have exactly one registered handler.</li>
 *   <li>No {@link CommandType} may have more than one handler (duplicates).</li>
 *   <li>No handler may declare support for a type that does not exist in the enum
 *       (this is structurally impossible given {@code supports(CommandType)} takes the enum,
 *       but is checked defensively via reflection in tests).</li>
 * </ul>
 *
 * <p>Reserved types ({@link CommandType#isReserved()}) are intentionally handler-less and are
 * skipped. They must never be dispatched at runtime.
 *
 * <p>If any violation is found the application fails immediately with a descriptive
 * {@link IllegalStateException} before it starts serving traffic.
 */
@Component
public class CommandHandlerRegistryValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CommandHandlerRegistryValidator.class);

    private final List<CommandHandler> handlers;

    public CommandHandlerRegistryValidator(List<CommandHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate(handlers);
    }

    /**
     * Core validation logic, extracted so it can be called directly from unit tests
     * without needing a full Spring context.
     *
     * @param handlers the list of all registered CommandHandler beans
     * @throws IllegalStateException if any validation rule is violated
     */
    public static void validate(List<CommandHandler> handlers) {
        // Build a map: CommandType -> list of handler class names that support it
        Map<CommandType, List<String>> handlersByType = Arrays.stream(CommandType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> handlers.stream()
                                .filter(h -> h.supports(type))
                                .map(h -> h.getClass().getSimpleName())
                                .collect(Collectors.toList())
                ));

        List<String> violations = new ArrayList<>();

        for (CommandType type : CommandType.values()) {
            List<String> supporting = handlersByType.get(type);

            if (type.isReserved()) {
                // Reserved types are intentionally handler-less.
                // Warn if someone accidentally registered a handler for one.
                if (!supporting.isEmpty()) {
                    violations.add(String.format(
                            "[RESERVED_WITH_HANDLER] %s is marked reserved but has %d handler(s): %s. " +
                            "Remove from RESERVED set or remove the handler.",
                            type, supporting.size(), supporting));
                } else {
                    log.info("CommandHandler registry: {} is RESERVED (no handler required — intentional)", type);
                }
                continue;
            }

            // Executable types: must have exactly one handler
            if (supporting.isEmpty()) {
                violations.add(String.format(
                        "[MISSING_HANDLER] %s has no registered handler. " +
                        "Expected: 1, Actual: 0. " +
                        "Add a @Component implementing CommandHandler that supports this type, " +
                        "or mark it as reserved via CommandType.RESERVED if not yet implemented.",
                        type));
            } else if (supporting.size() > 1) {
                violations.add(String.format(
                        "[DUPLICATE_HANDLER] %s has %d registered handlers. " +
                        "Expected: 1, Actual: %d. Handlers: %s. " +
                        "Remove duplicate @Component registrations.",
                        type, supporting.size(), supporting.size(), supporting));
            } else {
                log.debug("CommandHandler registry: {} -> {}", type, supporting.get(0));
            }
        }

        if (!violations.isEmpty()) {
            String report = "\n\nCommandHandler registry validation FAILED at startup:\n" +
                    violations.stream()
                            .map(v -> "  • " + v)
                            .collect(Collectors.joining("\n")) +
                    "\n";
            throw new IllegalStateException(report);
        }

        log.info("CommandHandler registry validated: {} executable types, {} reserved types, {} total handlers registered.",
                Arrays.stream(CommandType.values()).filter(t -> !t.isReserved()).count(),
                Arrays.stream(CommandType.values()).filter(CommandType::isReserved).count(),
                handlers.size());
    }
}
