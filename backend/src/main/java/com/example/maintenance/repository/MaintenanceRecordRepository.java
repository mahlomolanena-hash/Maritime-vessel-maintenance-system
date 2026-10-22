package com.example.maintenance.repository;

import com.example.maintenance.domain.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long>, JpaSpecificationExecutor<MaintenanceRecord> {

    @Override
    @EntityGraph(attributePaths = {"workOrder", "equipment", "equipment.vessel", "performedBy"})
    Page<MaintenanceRecord> findAll(Specification<MaintenanceRecord> spec, Pageable pageable);
}
