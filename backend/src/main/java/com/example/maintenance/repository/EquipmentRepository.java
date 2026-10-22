package com.example.maintenance.repository;

import com.example.maintenance.domain.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    @Override
    @EntityGraph(attributePaths = {"vessel", "parent"})
    Page<Equipment> findAll(Specification<Equipment> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"vessel", "parent"})
    List<Equipment> findByVesselIdOrderByNameAsc(Long vesselId);

    boolean existsByVesselIdAndTagNumberIgnoreCase(Long vesselId, String tagNumber);
    boolean existsByVesselIdAndTagNumberIgnoreCaseAndIdNot(Long vesselId, String tagNumber, Long id);
    boolean existsByParentId(Long parentId);
    boolean existsByVesselId(Long vesselId);
    long countByVesselId(Long vesselId);
}
