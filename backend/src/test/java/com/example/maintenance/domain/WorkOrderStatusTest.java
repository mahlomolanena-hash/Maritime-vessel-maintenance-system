package com.example.maintenance.domain;

import org.junit.jupiter.api.Test;

import static com.example.maintenance.domain.WorkOrderStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkOrderStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(OPEN.canTransitionTo(ASSIGNED));
        assertTrue(ASSIGNED.canTransitionTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canTransitionTo(ON_HOLD));
        assertTrue(ON_HOLD.canTransitionTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canTransitionTo(COMPLETED));
    }

    @Test
    void stepsCannotBeSkipped() {
        assertFalse(OPEN.canTransitionTo(IN_PROGRESS));
        assertFalse(OPEN.canTransitionTo(COMPLETED));
        assertFalse(ASSIGNED.canTransitionTo(COMPLETED));
        assertFalse(ON_HOLD.canTransitionTo(COMPLETED));
    }

    @Test
    void terminalStatesAreFinal() {
        for (WorkOrderStatus next : values()) {
            assertFalse(COMPLETED.canTransitionTo(next), "COMPLETED -> " + next);
            assertFalse(CANCELLED.canTransitionTo(next), "CANCELLED -> " + next);
        }
        assertTrue(COMPLETED.isTerminal());
        assertTrue(CANCELLED.isTerminal());
    }

    @Test
    void anyActiveStateCanBeCancelled() {
        for (WorkOrderStatus status : new WorkOrderStatus[]{OPEN, ASSIGNED, IN_PROGRESS, ON_HOLD}) {
            assertTrue(status.canTransitionTo(CANCELLED), status + " -> CANCELLED");
            assertTrue(status.isActive());
        }
    }

    @Test
    void assignedWorkCanBeReturnedToTheQueue() {
        assertEquals(true, ASSIGNED.canTransitionTo(OPEN));
    }
}
