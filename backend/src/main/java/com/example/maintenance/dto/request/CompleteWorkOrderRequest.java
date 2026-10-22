package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record CompleteWorkOrderRequest(
        @NotBlank(message = "Completion remarks are required") String completionRemarks,
        @DecimalMin(value = "0.0", message = "Actual hours cannot be negative") BigDecimal actualHours
) {
}
