package com.example.maintenance.repository;

import com.example.maintenance.domain.EquipmentHourReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentHourReadingRepository extends JpaRepository<EquipmentHourReading, Long> {

    @EntityGraph(attributePaths = {"equipment", "recordedBy"})
    Page<EquipmentHourReading> findByEquipmentIdOrderByReadingDateDescIdDesc(Long equipmentId, Pageable pageable);

    void deleteByEquipmentId(Long equipmentId);
}
