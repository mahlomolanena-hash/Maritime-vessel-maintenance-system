package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record ScheduleRequest(
        @NotNull(message = "Task is required") Long taskId,
        @NotNull(message = "Interval type is required") IntervalType intervalType,
        @NotNull(message = "Interval value is required") @Min(value = 1, message = "Interval must be at least 1") Integer intervalValue,
        LocalDate lastDoneDate,
        @DecimalMin(value = "0.0", message = "Hours cannot be negative") BigDecimal lastDoneHours,
        Boolean active
) {
}
