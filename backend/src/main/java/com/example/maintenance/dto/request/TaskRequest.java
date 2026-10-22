package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record TaskRequest(
        @NotNull(message = "Equipment is required") Long equipmentId,
        @NotBlank(message = "Task title is required") String title,
        String description,
        @DecimalMin(value = "0.0", message = "Estimated hours cannot be negative") BigDecimal estimatedHours,
        Boolean active
) {
}
