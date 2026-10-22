package com.example.maintenance.dto.response;

import com.example.maintenance.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Set<String> roles,
        boolean enabled
) {

    public static UserResponse from(User u) {
        Set<String> roles = new TreeSet<>();
        u.getRoles().forEach(r -> roles.add(r.name()));
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(), roles, u.isEnabled());
    }
}
