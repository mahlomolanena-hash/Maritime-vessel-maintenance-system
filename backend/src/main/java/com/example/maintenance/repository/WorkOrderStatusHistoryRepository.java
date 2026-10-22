package com.example.maintenance.repository;

import com.example.maintenance.domain.WorkOrderStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, Long> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<WorkOrderStatusHistory> findByWorkOrderIdOrderByChangedAtAscIdAsc(Long workOrderId);
}
