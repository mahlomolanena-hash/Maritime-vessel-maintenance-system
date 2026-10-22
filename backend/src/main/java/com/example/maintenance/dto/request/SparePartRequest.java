package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record SparePartRequest(
        @NotBlank(message = "Part number is required") String partNumber,
        @NotBlank(message = "Part name is required") String name,
        String description,
        String unit,
        @NotNull(message = "Quantity in stock is required") @Min(value = 0, message = "Quantity cannot be negative") Integer quantityInStock,
        @NotNull(message = "Reorder level is required") @Min(value = 0, message = "Reorder level cannot be negative") Integer reorderLevel,
        @NotNull(message = "Unit cost is required") @DecimalMin(value = "0.0", message = "Unit cost cannot be negative") BigDecimal unitCost,
        String storageLocation
) {
}
