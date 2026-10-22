package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record WorkOrderPartResponse(
        Long id,
        Long sparePartId,
        String partNumber,
        String partName,
        int quantityUsed,
        BigDecimal unitCostAtUse,
        BigDecimal lineCost
) {

    public static WorkOrderPartResponse from(WorkOrderPart p) {
        return new WorkOrderPartResponse(p.getId(), p.getSparePart().getId(), p.getSparePart().getPartNumber(),
                p.getSparePart().getName(), p.getQuantityUsed(), p.getUnitCostAtUse(), p.lineCost());
    }
}
