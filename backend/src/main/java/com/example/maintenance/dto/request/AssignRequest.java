package com.example.maintenance.dto.request;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record AssignRequest(
        @NotNull(message = "User is required") Long userId
) {
}
