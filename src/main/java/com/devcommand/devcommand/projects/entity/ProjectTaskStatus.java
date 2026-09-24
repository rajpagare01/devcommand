package com.devcommand.devcommand.projects.entity;

/**
 * Assumption: no enum values were given for ProjectTask.status, so the same
 * conventional three-state task lifecycle used elsewhere is applied here.
 */
public enum ProjectTaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
