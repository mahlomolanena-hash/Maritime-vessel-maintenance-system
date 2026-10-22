package com.example.maintenance.service;

import com.example.maintenance.domain.MaintenanceRecord;
import com.example.maintenance.domain.WorkOrderType;
import com.example.maintenance.dto.response.MaintenanceRecordResponse;
import com.example.maintenance.exception.ResourceNotFoundException;
import com.example.maintenance.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Read-only access to the maintenance history; records are only ever created by completing a work order. */
@Service
@RequiredArgsConstructor
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository recordRepository;

    @Transactional(readOnly = true)
    public Page<MaintenanceRecordResponse> search(Long vesselId, Long equipmentId, WorkOrderType type,
                                                  LocalDate from, LocalDate to, String q, Pageable pageable) {
        Specification<MaintenanceRecord> spec = (root, query, cb) -> cb.conjunction();
        if (vesselId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("equipment").get("vessel").get("id"), vesselId));
        }
        if (equipmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("equipment").get("id"), equipmentId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("workOrder").get("type"), type));
        }
        if (from != null) {
            Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("completedAt"), start));
        }
        if (to != null) {
            Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.<Instant>get("completedAt"), end));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("workOrder").get("title")), like),
                    cb.like(cb.lower(root.get("workPerformed")), like),
                    cb.like(cb.lower(root.get("workOrder").get("workOrderNumber")), like)));
        }
        return recordRepository.findAll(spec, pageable).map(MaintenanceRecordResponse::from);
    }

    @Transactional(readOnly = true)
    public MaintenanceRecordResponse get(Long id) {
        return MaintenanceRecordResponse.from(recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id " + id)));
    }
}
