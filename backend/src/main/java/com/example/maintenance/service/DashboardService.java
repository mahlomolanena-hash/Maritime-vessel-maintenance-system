package com.example.maintenance.service;

import com.example.maintenance.domain.DueStatus;
import com.example.maintenance.domain.MaintenanceSchedule;
import com.example.maintenance.domain.VesselStatus;
import com.example.maintenance.domain.WorkOrderStatus;
import com.example.maintenance.dto.response.DashboardResponse;
import com.example.maintenance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<WorkOrderStatus> ACTIVE = List.of(
            WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    private final VesselRepository vesselRepository;
    private final EquipmentRepository equipmentRepository;
    private final WorkOrderRepository workOrderRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final SparePartRepository sparePartRepository;
    private final ScheduleService scheduleService;

    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            byStatus.put(status.name(), workOrderRepository.countByStatus(status));
        }

        long overdueSchedules = 0;
        long dueSoonSchedules = 0;
        for (MaintenanceSchedule schedule : scheduleRepository.findAllActiveWithDetails()) {
            DueStatus status = scheduleService.dueStatusOf(schedule);
            if (status == DueStatus.OVERDUE) {
                overdueSchedules++;
            } else if (status == DueStatus.DUE_SOON) {
                dueSoonSchedules++;
            }
        }

        return new DashboardResponse(
                vesselRepository.countByStatus(VesselStatus.ACTIVE),
                equipmentRepository.count(),
                byStatus,
                workOrderRepository.countByStatusInAndDueDateBefore(ACTIVE, LocalDate.now()),
                overdueSchedules,
                dueSoonSchedules,
                sparePartRepository.countLowStock());
    }
}
