package com.example.maintenance.service;

import com.example.maintenance.domain.*;
import com.example.maintenance.dto.request.EquipmentRequest;
import com.example.maintenance.dto.request.HourReadingRequest;
import com.example.maintenance.dto.response.EquipmentResponse;
import com.example.maintenance.dto.response.HourReadingResponse;
import com.example.maintenance.exception.BadRequestException;
import com.example.maintenance.exception.ConflictException;
import com.example.maintenance.exception.DuplicateResourceException;
import com.example.maintenance.exception.ResourceNotFoundException;
import com.example.maintenance.repository.*;
import com.example.maintenance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentHourReadingRepository readingRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final WorkOrderRepository workOrderRepository;
    private final VesselRepository vesselRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<EquipmentResponse> search(Long vesselId, String q, EquipmentStatus status,
                                          Criticality criticality, Pageable pageable) {
        Specification<Equipment> spec = (root, query, cb) -> cb.conjunction();
        if (vesselId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("vessel").get("id"), vesselId));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("tagNumber")), like),
                    cb.like(cb.lower(root.get("serialNumber")), like)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (criticality != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("criticality"), criticality));
        }
        return equipmentRepository.findAll(spec, pageable).map(EquipmentResponse::from);
    }

    /** Every equipment item on a vessel as a flat list; clients build the parent/child tree from parentId. */
    @Transactional(readOnly = true)
    public List<EquipmentResponse> listForVessel(Long vesselId) {
        if (!vesselRepository.existsById(vesselId)) {
            throw new ResourceNotFoundException("Vessel not found with id " + vesselId);
        }
        return equipmentRepository.findByVesselIdOrderByNameAsc(vesselId).stream().map(EquipmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse get(Long id) {
        return EquipmentResponse.from(find(id));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest req) {
        Vessel vessel = vesselRepository.findById(req.vesselId())
                .orElseThrow(() -> new ResourceNotFoundException("Vessel not found with id " + req.vesselId()));
        if (equipmentRepository.existsByVesselIdAndTagNumberIgnoreCase(vessel.getId(), req.tagNumber().trim())) {
            throw new DuplicateResourceException("Tag number " + req.tagNumber() + " is already used on this vessel");
        }
        Equipment parent = resolveParent(req.parentId(), vessel.getId(), null);

        Equipment equipment = Equipment.builder()
                .vessel(vessel)
                .parent(parent)
                .name(req.name().trim())
                .tagNumber(req.tagNumber().trim())
                .category(req.category())
                .manufacturer(req.manufacturer())
                .model(req.model())
                .serialNumber(req.serialNumber())
                .location(req.location())
                .installedDate(req.installedDate())
                .status(req.status() != null ? req.status() : EquipmentStatus.OPERATIONAL)
                .criticality(req.criticality() != null ? req.criticality() : Criticality.MEDIUM)
                .build();
        return EquipmentResponse.from(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest req) {
        Equipment equipment = find(id);
        if (!equipment.getVessel().getId().equals(req.vesselId())) {
            throw new BadRequestException("Equipment cannot be moved to a different vessel");
        }
        if (equipmentRepository.existsByVesselIdAndTagNumberIgnoreCaseAndIdNot(
                equipment.getVessel().getId(), req.tagNumber().trim(), id)) {
            throw new DuplicateResourceException("Tag number " + req.tagNumber() + " is already used on this vessel");
        }
        equipment.setParent(resolveParent(req.parentId(), equipment.getVessel().getId(), id));
        equipment.setName(req.name().trim());
        equipment.setTagNumber(req.tagNumber().trim());
        equipment.setCategory(req.category());
        equipment.setManufacturer(req.manufacturer());
        equipment.setModel(req.model());
        equipment.setSerialNumber(req.serialNumber());
        equipment.setLocation(req.location());
        equipment.setInstalledDate(req.installedDate());
        if (req.status() != null) {
            equipment.setStatus(req.status());
        }
        if (req.criticality() != null) {
            equipment.setCriticality(req.criticality());
        }
        return EquipmentResponse.from(equipmentRepository.save(equipment));
    }

    @Transactional
    public void delete(Long id) {
        Equipment equipment = find(id);
        if (equipmentRepository.existsByParentId(id)) {
            throw new ConflictException("This equipment has sub-components. Remove or move them first.");
        }
        if (taskRepository.existsByEquipmentId(id) || workOrderRepository.existsByEquipmentId(id)) {
            throw new ConflictException("This equipment has maintenance tasks or work orders and cannot be deleted. Mark it OUT_OF_SERVICE instead.");
        }
        readingRepository.deleteByEquipmentId(id);
        equipmentRepository.delete(equipment);
    }

    /**
     * Records a new hour-meter reading. Meters only ever move forward: a lower value than the
     * current one is almost always a typo, and accepting it would corrupt every hour-based schedule.
     */
    @Transactional
    public HourReadingResponse recordReading(Long equipmentId, HourReadingRequest req, UserPrincipal principal) {
        Equipment equipment = find(equipmentId);
        BigDecimal hours = req.hours().setScale(1, RoundingMode.HALF_UP);
        BigDecimal current = equipment.hoursFor(req.meterType());

        if (hours.compareTo(current) < 0) {
            throw new BadRequestException("The " + req.meterType().name().toLowerCase()
                    + " hour meter cannot go backwards (current reading is " + current + ")");
        }
        LocalDate date = req.readingDate() != null ? req.readingDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Reading date cannot be in the future");
        }

        User recorder = userRepository.findById(principal.getId()).orElse(null);
        EquipmentHourReading reading = EquipmentHourReading.builder()
                .equipment(equipment)
                .meterType(req.meterType())
                .hours(hours)
                .readingDate(date)
                .recordedBy(recorder)
                .notes(req.notes())
                .build();
        readingRepository.save(reading);

        equipment.setHoursFor(req.meterType(), hours);
        equipmentRepository.save(equipment);
        return HourReadingResponse.from(reading);
    }

    @Transactional(readOnly = true)
    public Page<HourReadingResponse> readings(Long equipmentId, Pageable pageable) {
        if (!equipmentRepository.existsById(equipmentId)) {
            throw new ResourceNotFoundException("Equipment not found with id " + equipmentId);
        }
        return readingRepository.findByEquipmentIdOrderByReadingDateDescIdDesc(equipmentId, pageable)
                .map(HourReadingResponse::from);
    }

    Equipment find(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id " + id));
    }

    /** The parent must be on the same vessel and must not create a loop in the hierarchy. */
    private Equipment resolveParent(Long parentId, Long vesselId, Long selfId) {
        if (parentId == null) {
            return null;
        }
        Equipment parent = equipmentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent equipment not found with id " + parentId));
        if (!parent.getVessel().getId().equals(vesselId)) {
            throw new BadRequestException("Parent equipment must be on the same vessel");
        }
        for (Equipment cursor = parent; cursor != null; cursor = cursor.getParent()) {
            if (selfId != null && selfId.equals(cursor.getId())) {
                throw new BadRequestException("Equipment cannot be its own parent or a parent of its parent");
            }
        }
        return parent;
    }
}
