package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record VesselResponse(
        Long id,
        String name,
        String imoNumber,
        VesselType vesselType,
        String flag,
        Integer yearBuilt,
        BigDecimal grossTonnage,
        VesselStatus status,
        long equipmentCount,
        Instant createdAt,
        Instant updatedAt
) {

    public static VesselResponse from(Vessel v, long equipmentCount) {
        return new VesselResponse(v.getId(), v.getName(), v.getImoNumber(), v.getVesselType(), v.getFlag(), v.getYearBuilt(),
                v.getGrossTonnage(), v.getStatus(), equipmentCount, v.getCreatedAt(), v.getUpdatedAt());
    }
}
