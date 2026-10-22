package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record ScheduleResponse(
        Long id,
        Long taskId,
        String taskTitle,
        Long equipmentId,
        String equipmentName,
        Long vesselId,
        String vesselName,
        IntervalType intervalType,
        Integer intervalValue,
        LocalDate lastDoneDate,
        BigDecimal lastDoneHours,
        LocalDate nextDueDate,
        BigDecimal nextDueHours,
        boolean active,
        DueStatus dueStatus,
        Long daysRemaining,
        BigDecimal hoursRemaining
) {

    public static ScheduleResponse from(MaintenanceSchedule s, DueStatus dueStatus, Long daysRemaining, BigDecimal hoursRemaining) {
        Equipment e = s.getEquipment();
        return new ScheduleResponse(s.getId(), s.getTask().getId(), s.getTask().getTitle(), e.getId(), e.getName(),
                e.getVessel().getId(), e.getVessel().getName(), s.getIntervalType(), s.getIntervalValue(),
                s.getLastDoneDate(), s.getLastDoneHours(), s.getNextDueDate(), s.getNextDueHours(), s.isActive(),
                dueStatus, daysRemaining, hoursRemaining);
    }
}
