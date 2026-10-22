package com.example.maintenance.service;

import com.example.maintenance.domain.*;
import com.example.maintenance.dto.request.ScheduleRequest;
import com.example.maintenance.dto.response.ScheduleResponse;
import com.example.maintenance.exception.BadRequestException;
import com.example.maintenance.exception.ConflictException;
import com.example.maintenance.exception.ResourceNotFoundException;
import com.example.maintenance.repository.MaintenanceScheduleRepository;
import com.example.maintenance.repository.MaintenanceTaskRepository;
import com.example.maintenance.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Preventive maintenance schedules. A schedule says "repeat this task every N days / running hours /
 * operating hours". The due status is always computed from the live data (today's date, the equipment's
 * current hour meter) rather than stored, so it can never go stale.
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final MaintenanceScheduleRepository scheduleRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final WorkOrderRepository workOrderRepository;

    @Value("${app.maintenance.due-soon-days:7}")
    private int dueSoonDays;

    @Value("${app.maintenance.due-soon-hours:50}")
    private int dueSoonHours;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(Long equipmentId, Long vesselId, DueStatus dueStatus) {
        List<MaintenanceSchedule> schedules;
        if (equipmentId != null) {
            schedules = scheduleRepository.findByEquipmentIdWithDetails(equipmentId);
        } else if (vesselId != null) {
            schedules = scheduleRepository.findByVesselIdWithDetails(vesselId);
        } else {
            schedules = scheduleRepository.findAllWithDetails();
        }
        return schedules.stream()
                .map(this::toResponse)
                .filter(r -> dueStatus == null || r.dueStatus() == dueStatus)
                .sorted(byUrgency())
                .toList();
    }

    /** Active schedules that are overdue or due soon, most urgent first. */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> due(Long vesselId) {
        return scheduleRepository.findAllActiveWithDetails().stream()
                .filter(s -> vesselId == null || s.getEquipment().getVessel().getId().equals(vesselId))
                .map(this::toResponse)
                .filter(r -> r.dueStatus() != DueStatus.OK)
                .sorted(byUrgency())
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ScheduleResponse create(ScheduleRequest req) {
        MaintenanceTask task = taskRepository.findById(req.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + req.taskId()));
        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .task(task)
                .intervalType(req.intervalType())
                .intervalValue(req.intervalValue())
                .active(req.active() == null || req.active())
                .build();
        applyBaseline(schedule, req);
        MaintenanceSchedule saved = scheduleRepository.save(schedule);
        return toResponse(find(saved.getId()));
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest req) {
        MaintenanceSchedule schedule = find(id);
        if (!schedule.getTask().getId().equals(req.taskId())) {
            throw new BadRequestException("A schedule cannot be moved to a different task");
        }
        schedule.setIntervalType(req.intervalType());
        schedule.setIntervalValue(req.intervalValue());
        if (req.active() != null) {
            schedule.setActive(req.active());
        }
        applyBaseline(schedule, req);
        scheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    @Transactional
    public void delete(Long id) {
        MaintenanceSchedule schedule = find(id);
        if (workOrderRepository.existsByScheduleId(id)) {
            throw new ConflictException("Work orders were generated from this schedule. Deactivate it instead of deleting it.");
        }
        scheduleRepository.delete(schedule);
    }

    /**
     * Called when a work order generated from this schedule is completed: the clock restarts from
     * the completion date (calendar) or the equipment's current hour meter (hours).
     */
    @Transactional
    public void advanceAfterCompletion(MaintenanceSchedule schedule, LocalDate completedOn) {
        if (DueCalculator.isHourBased(schedule.getIntervalType())) {
            BigDecimal current = schedule.getEquipment().hoursFor(schedule.meter());
            schedule.setLastDoneHours(current);
            schedule.setNextDueHours(DueCalculator.nextDueHours(current, schedule.getIntervalValue()));
        } else {
            schedule.setLastDoneDate(completedOn);
            schedule.setNextDueDate(DueCalculator.nextDueDate(completedOn, schedule.getIntervalValue()));
        }
        scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public DueStatus dueStatusOf(MaintenanceSchedule s) {
        if (!s.isActive()) {
            return DueStatus.OK;
        }
        return DueCalculator.status(s.getIntervalType(), s.getNextDueDate(), s.getNextDueHours(),
                s.getEquipment().hoursFor(s.meter()), LocalDate.now(), dueSoonDays, BigDecimal.valueOf(dueSoonHours));
    }

    MaintenanceSchedule find(Long id) {
        return scheduleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + id));
    }

    private ScheduleResponse toResponse(MaintenanceSchedule s) {
        BigDecimal current = s.getEquipment().hoursFor(s.meter());
        LocalDate today = LocalDate.now();
        return ScheduleResponse.from(s, dueStatusOf(s),
                DueCalculator.daysRemaining(s.getIntervalType(), s.getNextDueDate(), today),
                DueCalculator.hoursRemaining(s.getIntervalType(), s.getNextDueHours(), current));
    }

    /**
     * Works out last-done / next-due from the request. Calendar schedules count from the given date
     * (default today); hour schedules count from the given meter value (default the current meter).
     */
    private void applyBaseline(MaintenanceSchedule schedule, ScheduleRequest req) {
        if (DueCalculator.isHourBased(req.intervalType())) {
            BigDecimal current = schedule.getTask().getEquipment().hoursFor(schedule.meter());
            BigDecimal last = req.lastDoneHours() != null
                    ? req.lastDoneHours().setScale(1, RoundingMode.HALF_UP)
                    : current;
            if (last.compareTo(current) > 0) {
                throw new BadRequestException("Last done hours (" + last + ") cannot be above the current meter reading (" + current + ")");
            }
            schedule.setLastDoneHours(last);
            schedule.setNextDueHours(DueCalculator.nextDueHours(last, req.intervalValue()));
            schedule.setLastDoneDate(null);
            schedule.setNextDueDate(null);
        } else {
            LocalDate last = req.lastDoneDate() != null ? req.lastDoneDate() : LocalDate.now();
            schedule.setLastDoneDate(last);
            schedule.setNextDueDate(DueCalculator.nextDueDate(last, req.intervalValue()));
            schedule.setLastDoneHours(null);
            schedule.setNextDueHours(null);
        }
    }

    /** Overdue first, then due soon, then OK; within a group the soonest due date comes first. */
    private Comparator<ScheduleResponse> byUrgency() {
        return Comparator
                .comparing((ScheduleResponse r) -> r.dueStatus().ordinal(), Comparator.reverseOrder())
                .thenComparing(r -> r.daysRemaining() == null ? Long.MAX_VALUE : r.daysRemaining())
                .thenComparing(r -> r.hoursRemaining() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : r.hoursRemaining());
    }
}
