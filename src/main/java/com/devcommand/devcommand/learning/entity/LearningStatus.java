package com.devcommand.devcommand.learning.entity;

/**
 * Original assumption (foundation stage): no enum values were specified
 * for LearningTopic.status, so a conventional three-state set
 * (NOT_STARTED/IN_PROGRESS/COMPLETED) was used.
 *
 * Entity correction made in this stage: ON_HOLD added. The Learning
 * Tracker module's spec explicitly requires it - the progress-update
 * endpoint's auto-transition rules need to distinguish "paused on
 * purpose" from the other states so they don't silently un-pause a topic
 * just because someone logged more hours against it. This is additive
 * (existing values unchanged), so no data migration concern.
 */
public enum LearningStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    ON_HOLD
}
