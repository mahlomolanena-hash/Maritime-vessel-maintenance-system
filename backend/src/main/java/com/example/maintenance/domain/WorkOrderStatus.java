package com.example.maintenance.domain;

/**
 * Work order lifecycle. The allowed transitions live here, in one place, so the service
 * layer never has to scatter "if status == ..." checks around.
 *
 *   OPEN -> ASSIGNED -> IN_PROGRESS <-> ON_HOLD
 *                           |
 *                       COMPLETED          (any non-terminal state -> CANCELLED)
 */
public enum WorkOrderStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(WorkOrderStatus next) {
        return switch (this) {
            case OPEN -> next == ASSIGNED || next == CANCELLED;
            case ASSIGNED -> next == IN_PROGRESS || next == OPEN || next == CANCELLED;
            case IN_PROGRESS -> next == ON_HOLD || next == COMPLETED || next == CANCELLED;
            case ON_HOLD -> next == IN_PROGRESS || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** Still needs work: anything that is not completed or cancelled. */
    public boolean isActive() {
        return !isTerminal();
    }
}
