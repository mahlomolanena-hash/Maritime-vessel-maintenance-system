package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record HourReadingResponse(
        Long id,
        Long equipmentId,
        MeterType meterType,
        BigDecimal hours,
        LocalDate readingDate,
        String recordedBy,
        String notes,
        Instant createdAt
) {

    public static HourReadingResponse from(EquipmentHourReading r) {
        return new HourReadingResponse(r.getId(), r.getEquipment().getId(), r.getMeterType(), r.getHours(), r.getReadingDate(),
                r.getRecordedBy() == null ? null : r.getRecordedBy().getFullName(), r.getNotes(), r.getCreatedAt());
    }
}
