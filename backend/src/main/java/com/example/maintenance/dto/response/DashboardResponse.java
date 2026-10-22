package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record DashboardResponse(
        long activeVessels,
        long totalEquipment,
        Map<String, Long> workOrdersByStatus,
        long overdueWorkOrders,
        long overdueSchedules,
        long dueSoonSchedules,
        long lowStockParts
) {
}
