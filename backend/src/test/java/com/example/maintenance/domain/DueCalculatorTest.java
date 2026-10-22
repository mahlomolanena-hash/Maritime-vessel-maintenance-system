package com.example.maintenance.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DueCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final BigDecimal SOON_HOURS = new BigDecimal("50");

    private DueStatus calendar(LocalDate nextDue) {
        return DueCalculator.status(IntervalType.CALENDAR_DAYS, nextDue, null, null, TODAY, 7, SOON_HOURS);
    }

    private DueStatus hours(IntervalType type, String nextDue, String current) {
        return DueCalculator.status(type, null, new BigDecimal(nextDue), new BigDecimal(current), TODAY, 7, SOON_HOURS);
    }

    @Test
    void calendar_pastDueDateIsOverdue() {
        assertEquals(DueStatus.OVERDUE, calendar(TODAY.minusDays(1)));
    }

    @Test
    void calendar_dueTodayIsDueSoonNotOverdue() {
        assertEquals(DueStatus.DUE_SOON, calendar(TODAY));
    }

    @Test
    void calendar_boundaryOfDueSoonWindow() {
        assertEquals(DueStatus.DUE_SOON, calendar(TODAY.plusDays(7)));
        assertEquals(DueStatus.OK, calendar(TODAY.plusDays(8)));
    }

    @Test
    void hours_reachingTheLimitIsOverdue() {
        assertEquals(DueStatus.OVERDUE, hours(IntervalType.RUNNING_HOURS, "1000", "1000.0"));
        assertEquals(DueStatus.OVERDUE, hours(IntervalType.RUNNING_HOURS, "1000", "1200.5"));
    }

    @Test
    void hours_dueSoonWindowIsInclusive() {
        assertEquals(DueStatus.DUE_SOON, hours(IntervalType.OPERATING_HOURS, "1000", "950.0"));
        assertEquals(DueStatus.OK, hours(IntervalType.OPERATING_HOURS, "1000", "949.9"));
    }

    @Test
    void missingBaselineIsTreatedAsOkInsteadOfFailing() {
        assertEquals(DueStatus.OK, DueCalculator.status(IntervalType.RUNNING_HOURS, null, null, BigDecimal.TEN, TODAY, 7, SOON_HOURS));
        assertEquals(DueStatus.OK, DueCalculator.status(IntervalType.CALENDAR_DAYS, null, null, null, TODAY, 7, SOON_HOURS));
    }

    @Test
    void nextDueIsLastDonePlusInterval() {
        assertEquals(LocalDate.of(2026, 11, 18), DueCalculator.nextDueDate(TODAY, 30));
        assertEquals(new BigDecimal("1250.0"), DueCalculator.nextDueHours(new BigDecimal("1000.0"), 250));
    }

    @Test
    void remainingValuesAreNegativeWhenOverdueAndNullForTheOtherScheduleKind() {
        assertEquals(-3L, DueCalculator.daysRemaining(IntervalType.CALENDAR_DAYS, TODAY.minusDays(3), TODAY));
        assertNull(DueCalculator.daysRemaining(IntervalType.RUNNING_HOURS, TODAY, TODAY));
        assertEquals(new BigDecimal("-20.5"),
                DueCalculator.hoursRemaining(IntervalType.RUNNING_HOURS, new BigDecimal("1000"), new BigDecimal("1020.5")));
        assertNull(DueCalculator.hoursRemaining(IntervalType.CALENDAR_DAYS, BigDecimal.ONE, BigDecimal.ONE));
    }
}
