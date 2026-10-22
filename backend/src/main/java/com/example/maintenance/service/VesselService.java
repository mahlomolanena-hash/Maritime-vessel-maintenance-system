package com.example.maintenance.service;

import com.example.maintenance.domain.Vessel;
import com.example.maintenance.domain.VesselStatus;
import com.example.maintenance.dto.request.VesselRequest;
import com.example.maintenance.dto.response.VesselResponse;
import com.example.maintenance.exception.ConflictException;
import com.example.maintenance.exception.DuplicateResourceException;
import com.example.maintenance.exception.ResourceNotFoundException;
import com.example.maintenance.repository.EquipmentRepository;
import com.example.maintenance.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VesselService {

    private final VesselRepository vesselRepository;
    private final EquipmentRepository equipmentRepository;

    @Transactional(readOnly = true)
    public Page<VesselResponse> search(String q, VesselStatus status, Pageable pageable) {
        Specification<Vessel> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(root.get("imoNumber"), like)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return vesselRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VesselResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public VesselResponse create(VesselRequest req) {
        if (vesselRepository.existsByImoNumber(req.imoNumber())) {
            throw new DuplicateResourceException("A vessel with IMO number " + req.imoNumber() + " already exists");
        }
        Vessel vessel = Vessel.builder()
                .name(req.name().trim())
                .imoNumber(req.imoNumber())
                .vesselType(req.vesselType())
                .flag(req.flag())
                .yearBuilt(req.yearBuilt())
                .grossTonnage(req.grossTonnage())
                .status(req.status() != null ? req.status() : VesselStatus.ACTIVE)
                .build();
        return toResponse(vesselRepository.save(vessel));
    }

    @Transactional
    public VesselResponse update(Long id, VesselRequest req) {
        Vessel vessel = find(id);
        if (vesselRepository.existsByImoNumberAndIdNot(req.imoNumber(), id)) {
            throw new DuplicateResourceException("A vessel with IMO number " + req.imoNumber() + " already exists");
        }
        vessel.setName(req.name().trim());
        vessel.setImoNumber(req.imoNumber());
        vessel.setVesselType(req.vesselType());
        vessel.setFlag(req.flag());
        vessel.setYearBuilt(req.yearBuilt());
        vessel.setGrossTonnage(req.grossTonnage());
        if (req.status() != null) {
            vessel.setStatus(req.status());
        }
        return toResponse(vesselRepository.save(vessel));
    }

    @Transactional
    public void delete(Long id) {
        Vessel vessel = find(id);
        if (equipmentRepository.existsByVesselId(id)) {
            throw new ConflictException("This vessel still has equipment. Set its status to DECOMMISSIONED instead of deleting it.");
        }
        vesselRepository.delete(vessel);
    }

    Vessel find(Long id) {
        return vesselRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel not found with id " + id));
    }

    private VesselResponse toResponse(Vessel v) {
        return VesselResponse.from(v, equipmentRepository.countByVesselId(v.getId()));
    }
}
