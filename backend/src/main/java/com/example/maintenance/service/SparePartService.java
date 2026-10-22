package com.example.maintenance.service;

import com.example.maintenance.domain.SparePart;
import com.example.maintenance.dto.request.SparePartRequest;
import com.example.maintenance.dto.request.StockAdjustRequest;
import com.example.maintenance.dto.response.SparePartResponse;
import com.example.maintenance.exception.ConflictException;
import com.example.maintenance.exception.DuplicateResourceException;
import com.example.maintenance.exception.InsufficientStockException;
import com.example.maintenance.exception.ResourceNotFoundException;
import com.example.maintenance.repository.SparePartRepository;
import com.example.maintenance.repository.WorkOrderPartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SparePartService {

    private final SparePartRepository sparePartRepository;
    private final WorkOrderPartRepository workOrderPartRepository;

    @Transactional(readOnly = true)
    public Page<SparePartResponse> search(String q, boolean lowStockOnly, Pageable pageable) {
        Specification<SparePart> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("partNumber")), like)));
        }
        if (lowStockOnly) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.<Integer>get("quantityInStock"), root.<Integer>get("reorderLevel")));
        }
        return sparePartRepository.findAll(spec, pageable).map(SparePartResponse::from);
    }

    @Transactional(readOnly = true)
    public SparePartResponse get(Long id) {
        return SparePartResponse.from(find(id));
    }

    @Transactional
    public SparePartResponse create(SparePartRequest req) {
        if (sparePartRepository.existsByPartNumberIgnoreCase(req.partNumber().trim())) {
            throw new DuplicateResourceException("Part number " + req.partNumber().trim() + " already exists");
        }
        SparePart part = SparePart.builder()
                .partNumber(req.partNumber().trim())
                .name(req.name().trim())
                .description(req.description())
                .unit(req.unit() == null || req.unit().isBlank() ? "pcs" : req.unit().trim())
                .quantityInStock(req.quantityInStock())
                .reorderLevel(req.reorderLevel())
                .unitCost(req.unitCost())
                .storageLocation(req.storageLocation())
                .build();
        return SparePartResponse.from(sparePartRepository.save(part));
    }

    @Transactional
    public SparePartResponse update(Long id, SparePartRequest req) {
        SparePart part = find(id);
        if (sparePartRepository.existsByPartNumberIgnoreCaseAndIdNot(req.partNumber().trim(), id)) {
            throw new DuplicateResourceException("Part number " + req.partNumber().trim() + " already exists");
        }
        part.setPartNumber(req.partNumber().trim());
        part.setName(req.name().trim());
        part.setDescription(req.description());
        part.setUnit(req.unit() == null || req.unit().isBlank() ? "pcs" : req.unit().trim());
        part.setQuantityInStock(req.quantityInStock());
        part.setReorderLevel(req.reorderLevel());
        part.setUnitCost(req.unitCost());
        part.setStorageLocation(req.storageLocation());
        return SparePartResponse.from(sparePartRepository.save(part));
    }

    /** Receive stock (positive delta) or write stock off (negative delta). */
    @Transactional
    public SparePartResponse adjustStock(Long id, StockAdjustRequest req) {
        SparePart part = find(id);
        int updated = part.getQuantityInStock() + req.delta();
        if (updated < 0) {
            throw new InsufficientStockException("Only " + part.getQuantityInStock() + " " + part.getUnit()
                    + " of " + part.getPartNumber() + " in stock; cannot remove " + Math.abs(req.delta()));
        }
        part.setQuantityInStock(updated);
        log.info("Stock adjusted for {} by {} (reason: {})", part.getPartNumber(), req.delta(), req.reason());
        return SparePartResponse.from(sparePartRepository.save(part));
    }

    @Transactional
    public void delete(Long id) {
        SparePart part = find(id);
        if (workOrderPartRepository.existsBySparePartId(id)) {
            throw new ConflictException("This part has been used on work orders and cannot be deleted");
        }
        sparePartRepository.delete(part);
    }

    SparePart find(Long id) {
        return sparePartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spare part not found with id " + id));
    }
}
