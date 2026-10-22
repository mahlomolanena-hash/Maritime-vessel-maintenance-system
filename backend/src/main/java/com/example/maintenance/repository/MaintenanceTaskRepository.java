package com.example.maintenance.repository;

import com.example.maintenance.domain.MaintenanceTask;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {

    @EntityGraph(attributePaths = {"equipment"})
    List<MaintenanceTask> findByEquipmentIdOrderByTitleAsc(Long equipmentId);

    @EntityGraph(attributePaths = {"equipment"})
    List<MaintenanceTask> findAllByOrderByTitleAsc();

    boolean existsByEquipmentIdAndTitleIgnoreCase(Long equipmentId, String title);
    boolean existsByEquipmentIdAndTitleIgnoreCaseAndIdNot(Long equipmentId, String title, Long id);
    boolean existsByEquipmentId(Long equipmentId);
}
