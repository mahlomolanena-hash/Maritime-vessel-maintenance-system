package com.example.maintenance.repository;

import com.example.maintenance.domain.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {

    String FETCH = "select s from MaintenanceSchedule s join fetch s.task t join fetch t.equipment e join fetch e.vessel v ";

    @Query(FETCH + "order by s.id")
    List<MaintenanceSchedule> findAllWithDetails();

    @Query(FETCH + "where s.active = true order by s.id")
    List<MaintenanceSchedule> findAllActiveWithDetails();

    @Query(FETCH + "where e.id = :equipmentId order by s.id")
    List<MaintenanceSchedule> findByEquipmentIdWithDetails(@Param("equipmentId") Long equipmentId);

    @Query(FETCH + "where v.id = :vesselId order by s.id")
    List<MaintenanceSchedule> findByVesselIdWithDetails(@Param("vesselId") Long vesselId);

    @Query(FETCH + "where s.id = :id")
    Optional<MaintenanceSchedule> findByIdWithDetails(@Param("id") Long id);

    boolean existsByTaskId(Long taskId);
}
