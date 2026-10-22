package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record SparePartResponse(
        Long id,
        String partNumber,
        String name,
        String description,
        String unit,
        int quantityInStock,
        int reorderLevel,
        BigDecimal unitCost,
        String storageLocation,
        boolean lowStock
) {

    public static SparePartResponse from(SparePart p) {
        return new SparePartResponse(p.getId(), p.getPartNumber(), p.getName(), p.getDescription(), p.getUnit(),
                p.getQuantityInStock(), p.getReorderLevel(), p.getUnitCost(), p.getStorageLocation(), p.isLowStock());
    }
}
