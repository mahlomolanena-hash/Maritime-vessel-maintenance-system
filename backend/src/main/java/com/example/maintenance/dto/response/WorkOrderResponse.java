package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record WorkOrderResponse(
        Long id,
        String workOrderNumber,
        Long equipmentId,
        String equipmentName,
        Long vesselId,
        String vesselName,
        Long taskId,
        Long scheduleId,
        String title,
        String description,
        WorkOrderType type,
        WorkOrderPriority priority,
        WorkOrderStatus status,
        Long createdById,
        String createdByName,
        Long assignedToId,
        String assignedToName,
        LocalDate dueDate,
        boolean overdue,
        Instant startedAt,
        Instant completedAt,
        String completionRemarks,
        BigDecimal actualHours,
        BigDecimal partsCost,
        List<WorkOrderPartResponse> parts,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkOrderResponse from(WorkOrder w, boolean includeParts) {
        Equipment e = w.getEquipment();
        User assignee = w.getAssignedTo();
        List<WorkOrderPartResponse> parts = includeParts
                ? w.getParts().stream().map(WorkOrderPartResponse::from).toList()
                : List.of();
        return new WorkOrderResponse(w.getId(), w.getWorkOrderNumber(), e.getId(), e.getName(), e.getVessel().getId(),
                e.getVessel().getName(), w.getTask() == null ? null : w.getTask().getId(),
                w.getSchedule() == null ? null : w.getSchedule().getId(), w.getTitle(), w.getDescription(), w.getType(),
                w.getPriority(), w.getStatus(), w.getCreatedBy().getId(), w.getCreatedBy().getFullName(),
                assignee == null ? null : assignee.getId(), assignee == null ? null : assignee.getFullName(),
                w.getDueDate(), w.isOverdue(), w.getStartedAt(), w.getCompletedAt(), w.getCompletionRemarks(),
                w.getActualHours(), includeParts ? w.partsCost() : null, parts, w.getCreatedAt(), w.getUpdatedAt());
    }
}
