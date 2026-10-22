package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record TaskResponse(
        Long id,
        Long equipmentId,
        String equipmentName,
        String title,
        String description,
        BigDecimal estimatedHours,
        boolean active
) {

    public static TaskResponse from(MaintenanceTask t) {
        return new TaskResponse(t.getId(), t.getEquipment().getId(), t.getEquipment().getName(), t.getTitle(),
                t.getDescription(), t.getEstimatedHours(), t.isActive());
    }
}
