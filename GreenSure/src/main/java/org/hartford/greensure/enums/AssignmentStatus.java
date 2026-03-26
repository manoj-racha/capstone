package org.hartford.greensure.enums;

/**
 * Lifecycle status of an AgentAssignment.
 * Replaces the two overlapping enums (AssignmentStatus + AssignmentLifecycleStatus)
 * that existed in the old design. A single enum is cleaner and avoids ambiguity.
 *
 * ACTIVE      — agent has been assigned, deadline clock is running
 * COMPLETED   — agent submitted a verification outcome (confirm/modify/reject)
 * REASSIGNED  — assignment was superseded by a new assignment
 *               (used when deadline missed or admin manually reassigns)
 * CANCELLED   — admin explicitly cancelled the assignment;
 *               declaration returns to SUBMITTED status
 */
public enum AssignmentStatus {

    ACTIVE,
    COMPLETED,
    REASSIGNED,
    CANCELLED;

    /** Convenience — true only when the assignment is still open. */
    public boolean isActive() {
        return this == ACTIVE;
    }
}
