package com.example.maintenance.repository;

import com.example.maintenance.domain.WorkOrder;
import com.example.maintenance.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {

    @Override
    @EntityGraph(attributePaths = {"equipment", "equipment.vessel", "assignedTo", "createdBy"})
    Page<WorkOrder> findAll(Specification<WorkOrder> spec, Pageable pageable);

    boolean existsByEquipmentId(Long equipmentId);
    boolean existsByTaskId(Long taskId);
    boolean existsByScheduleId(Long scheduleId);
    boolean existsByScheduleIdAndStatusIn(Long scheduleId, Collection<WorkOrderStatus> statuses);
    long countByStatus(WorkOrderStatus status);
    long countByStatusInAndDueDateBefore(Collection<WorkOrderStatus> statuses, LocalDate date);
}
