package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record EquipmentResponse(
        Long id,
        Long vesselId,
        String vesselName,
        Long parentId,
        String parentName,
        String name,
        String tagNumber,
        String category,
        String manufacturer,
        String model,
        String serialNumber,
        String location,
        LocalDate installedDate,
        EquipmentStatus status,
        Criticality criticality,
        BigDecimal runningHours,
        BigDecimal operatingHours
) {

    public static EquipmentResponse from(Equipment e) {
        Equipment p = e.getParent();
        return new EquipmentResponse(e.getId(), e.getVessel().getId(), e.getVessel().getName(),
                p == null ? null : p.getId(), p == null ? null : p.getName(),
                e.getName(), e.getTagNumber(), e.getCategory(), e.getManufacturer(), e.getModel(), e.getSerialNumber(),
                e.getLocation(), e.getInstalledDate(), e.getStatus(), e.getCriticality(), e.getRunningHours(), e.getOperatingHours());
    }
}
