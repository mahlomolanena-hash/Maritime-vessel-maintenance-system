package com.example.maintenance.repository;

import com.example.maintenance.domain.WorkOrderPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderPartRepository extends JpaRepository<WorkOrderPart, Long> {
    boolean existsBySparePartId(Long sparePartId);
}
