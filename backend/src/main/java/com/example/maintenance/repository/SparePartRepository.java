package com.example.maintenance.repository;

import com.example.maintenance.domain.SparePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SparePartRepository extends JpaRepository<SparePart, Long>, JpaSpecificationExecutor<SparePart> {
    boolean existsByPartNumberIgnoreCase(String partNumber);
    boolean existsByPartNumberIgnoreCaseAndIdNot(String partNumber, Long id);

    @Query("select count(p) from SparePart p where p.quantityInStock <= p.reorderLevel")
    long countLowStock();
}
