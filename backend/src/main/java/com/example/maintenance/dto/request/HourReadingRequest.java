package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record HourReadingRequest(
        @NotNull(message = "Meter type is required") MeterType meterType,
        @NotNull(message = "Hours are required") @DecimalMin(value = "0.0", message = "Hours cannot be negative") BigDecimal hours,
        LocalDate readingDate,
        String notes
) {
}
