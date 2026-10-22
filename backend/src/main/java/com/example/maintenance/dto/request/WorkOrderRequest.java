package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record WorkOrderRequest(
        @NotNull(message = "Equipment is required") Long equipmentId,
        Long taskId,
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Work order type is required") WorkOrderType type,
        @NotNull(message = "Priority is required") WorkOrderPriority priority,
        LocalDate dueDate,
        Long assignedToId
) {
}
