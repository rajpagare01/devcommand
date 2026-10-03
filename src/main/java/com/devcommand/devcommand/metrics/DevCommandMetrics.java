package com.devcommand.devcommand.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Central metrics facade for the DevCommand platform.
 *
 * <p>All metric labels are safe: no user IDs, Telegram IDs, emails, JWTs, tokens, or API keys.
 * Label values are always static strings (command type names, result types) — not runtime user data.
 *
 * <p>Metrics exposed:
 * <ul>
 *   <li>{@code devcommand.command.dispatched}  — tagged by {command_type}</li>
 *   <li>{@code devcommand.command.success}     — tagged by {command_type}</li>
 *   <li>{@code devcommand.command.failure}     — tagged by {command_type}</li>
 *   <li>{@code devcommand.ratelimit.rejected}  — tagged by {endpoint}</li>
 *   <li>{@code devcommand.gemini.failure}      — tagged by {reason}</li>
 *   <li>{@code devcommand.telegram.processing.failure} — no sensitive tags</li>
 * </ul>
 */
@Component
public class DevCommandMetrics {

    private final MeterRegistry registry;

    public DevCommandMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    // ------------------------------------------------------------------ Command metrics

    /**
     * Records a dispatched command. Call before dispatching so partial failures are visible.
     *
     * @param commandTypeName the {@link com.devcommand.devcommand.command.CommandType#name()} value
     */
    public void recordCommandDispatched(String commandTypeName) {
        counter("devcommand.command.dispatched", "command_type", safe(commandTypeName)).increment();
    }

    /**
     * Records a successful command execution.
     */
    public void recordCommandSuccess(String commandTypeName) {
        counter("devcommand.command.success", "command_type", safe(commandTypeName)).increment();
    }

    /**
     * Records a failed command execution (CommandResult.isSuccess() == false).
     */
    public void recordCommandFailure(String commandTypeName) {
        counter("devcommand.command.failure", "command_type", safe(commandTypeName)).increment();
    }

    // ------------------------------------------------------------------ Rate limit metrics

    /**
     * Records a rate-limit rejection.
     *
     * @param endpointLabel the endpoint label used in the bucket key (e.g. "auth:login").
     *                      Never the full path or any user-identifying information.
     */
    public void recordRateLimitRejection(String endpointLabel) {
        counter("devcommand.ratelimit.rejected", "endpoint", safe(endpointLabel)).increment();
    }

    // ------------------------------------------------------------------ Gemini metrics

    /**
     * Records a Gemini interpretation failure.
     *
     * @param reason a safe, static reason label (e.g. "MISSING_API_KEY", "TIMEOUT", "INVALID_RESPONSE")
     */
    public void recordGeminiFailure(String reason) {
        counter("devcommand.gemini.failure", "reason", safe(reason)).increment();
    }

    // ------------------------------------------------------------------ Telegram metrics

    /**
     * Records a Telegram message processing failure.
     * No sensitive tags — the failure category is sufficient for alerting.
     */
    public void recordTelegramProcessingFailure() {
        counter("devcommand.telegram.processing.failure").increment();
    }

    // ------------------------------------------------------------------ Helpers

    private Counter counter(String name, String... tags) {
        return Counter.builder(name)
                .tags(tags)
                .register(registry);
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(registry);
    }

    /**
     * Sanitises label values: replaces any null with "unknown" and truncates at 64 chars
     * to prevent unbounded cardinality from label injection via malformed input.
     */
    private String safe(String label) {
        if (label == null) return "unknown";
        String trimmed = label.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
