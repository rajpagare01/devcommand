package com.devcommand.devcommand.command;

import java.util.EnumSet;
import java.util.Set;

public enum CommandType {
    CREATE_TASK,
    COMPLETE_TASK,
    CREATE_DSA_PROBLEM,
    UPDATE_LEARNING_PROGRESS,
    CREATE_JOB_APPLICATION,

    /**
     * Reserved: enum constant is defined for future use but no handler is registered yet.
     * The Gemini allowlist also excludes this type. Remove from RESERVED and add a handler
     * when implementation begins.
     */
    READ_TASKS_TODAY,
    READ_PENDING_TASKS,
    READ_DSA_STATS,
    READ_JOB_PIPELINE,
    READ_LEARNING_PROGRESS,

    DELETE_TASK,
    CONFIRM_ACTION,
    CANCEL_ACTION;

    /**
     * Command types that are intentionally defined but do not yet have a registered handler.
     * The startup validator skips these rather than treating them as missing-handler bugs.
     */
    private static final Set<CommandType> RESERVED = EnumSet.of(READ_TASKS_TODAY);

    /**
     * Returns true if this command type is reserved (intentionally handler-less).
     * Reserved types must NOT be dispatched at runtime.
     */
    public boolean isReserved() {
        return RESERVED.contains(this);
    }
}
