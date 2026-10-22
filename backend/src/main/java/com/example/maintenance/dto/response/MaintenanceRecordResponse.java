package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record MaintenanceRecordResponse(
        Long id,
        Long workOrderId,
        String workOrderNumber,
        String title,
        WorkOrderType type,
        Long equipmentId,
        String equipmentName,
        Long vesselId,
        String vesselName,
        String performedBy,
        Instant completedAt,
        BigDecimal runningHoursAtService,
        BigDecimal operatingHoursAtService,
        BigDecimal actualHours,
        BigDecimal partsCost,
        String workPerformed
) {

    public static MaintenanceRecordResponse from(MaintenanceRecord r) {
        Equipment e = r.getEquipment();
        WorkOrder w = r.getWorkOrder();
        return new MaintenanceRecordResponse(r.getId(), w.getId(), w.getWorkOrderNumber(), w.getTitle(), w.getType(),
                e.getId(), e.getName(), e.getVessel().getId(), e.getVessel().getName(),
                r.getPerformedBy() == null ? null : r.getPerformedBy().getFullName(), r.getCompletedAt(),
                r.getRunningHoursAtService(), r.getOperatingHoursAtService(), r.getActualHours(), r.getPartsCost(),
                r.getWorkPerformed());
    }
}
