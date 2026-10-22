package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record EquipmentRequest(
        @NotNull(message = "Vessel is required") Long vesselId,
        Long parentId,
        @NotBlank(message = "Equipment name is required") String name,
        @NotBlank(message = "Tag number is required") String tagNumber,
        String category,
        String manufacturer,
        String model,
        String serialNumber,
        String location,
        LocalDate installedDate,
        EquipmentStatus status,
        Criticality criticality
) {
}
