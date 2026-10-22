package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record VesselRequest(
        @NotBlank(message = "Vessel name is required") String name,
        @NotBlank(message = "IMO number is required") @Pattern(regexp = "\\d{7}", message = "IMO number must be exactly 7 digits") String imoNumber,
        @NotNull(message = "Vessel type is required") VesselType vesselType,
        String flag,
        @Min(value = 1800, message = "Year built is not valid") @Max(value = 2100, message = "Year built is not valid") Integer yearBuilt,
        @DecimalMin(value = "0.0", message = "Gross tonnage cannot be negative") BigDecimal grossTonnage,
        VesselStatus status
) {
}
