package com.example.maintenance.repository;

import com.example.maintenance.domain.Vessel;
import com.example.maintenance.domain.VesselStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VesselRepository extends JpaRepository<Vessel, Long>, JpaSpecificationExecutor<Vessel> {
    boolean existsByImoNumber(String imoNumber);
    boolean existsByImoNumberAndIdNot(String imoNumber, Long id);
    long countByStatus(VesselStatus status);
}
