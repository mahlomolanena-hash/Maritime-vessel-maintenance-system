package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record WorkOrderHistoryResponse(
        Long id,
        WorkOrderStatus fromStatus,
        WorkOrderStatus toStatus,
        String changedBy,
        String notes,
        Instant changedAt
) {

    public static WorkOrderHistoryResponse from(WorkOrderStatusHistory h) {
        return new WorkOrderHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getChangedBy() == null ? null : h.getChangedBy().getFullName(), h.getNotes(), h.getChangedAt());
    }
}
