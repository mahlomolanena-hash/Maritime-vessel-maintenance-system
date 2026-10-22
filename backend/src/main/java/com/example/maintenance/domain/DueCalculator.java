package com.example.maintenance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pure due-date / due-hours logic for preventive maintenance schedules.
 * No Spring, no database: easy to unit test and reuse from the dashboard.
 */
public final class DueCalculator {

    private DueCalculator() {
    }

    public static LocalDate nextDueDate(LocalDate lastDone, int intervalDays) {
        return lastDone.plusDays(intervalDays);
    }

    public static BigDecimal nextDueHours(BigDecimal lastDoneHours, int intervalHours) {
        return lastDoneHours.add(BigDecimal.valueOf(intervalHours));
    }

    public static boolean isHourBased(IntervalType type) {
        return type == IntervalType.RUNNING_HOURS || type == IntervalType.OPERATING_HOURS;
    }

    public static DueStatus status(IntervalType type,
                                   LocalDate nextDueDate,
                                   BigDecimal nextDueHours,
                                   BigDecimal currentHours,
                                   LocalDate today,
                                   int dueSoonDays,
                                   BigDecimal dueSoonHours) {
        if (isHourBased(type)) {
            if (nextDueHours == null || currentHours == null) {
                return DueStatus.OK;
            }
            BigDecimal remaining = nextDueHours.subtract(currentHours);
            if (remaining.signum() <= 0) {
                return DueStatus.OVERDUE;
            }
            return remaining.compareTo(dueSoonHours) <= 0 ? DueStatus.DUE_SOON : DueStatus.OK;
        }

        if (nextDueDate == null) {
            return DueStatus.OK;
        }
        if (today.isAfter(nextDueDate)) {
            return DueStatus.OVERDUE;
        }
        return !today.plusDays(dueSoonDays).isBefore(nextDueDate) ? DueStatus.DUE_SOON : DueStatus.OK;
    }

    /** Days until due (negative when overdue); null for hour-based schedules. */
    public static Long daysRemaining(IntervalType type, LocalDate nextDueDate, LocalDate today) {
        if (isHourBased(type) || nextDueDate == null) {
            return null;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(today, nextDueDate);
    }

    /** Hours until due (negative when overdue); null for calendar schedules. */
    public static BigDecimal hoursRemaining(IntervalType type, BigDecimal nextDueHours, BigDecimal currentHours) {
        if (!isHourBased(type) || nextDueHours == null || currentHours == null) {
            return null;
        }
        return nextDueHours.subtract(currentHours);
    }
}
