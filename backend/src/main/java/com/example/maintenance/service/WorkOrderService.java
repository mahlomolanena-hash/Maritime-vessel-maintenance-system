package com.example.maintenance.service;

import com.example.maintenance.domain.*;
import com.example.maintenance.dto.request.*;
import com.example.maintenance.dto.response.WorkOrderHistoryResponse;
import com.example.maintenance.dto.response.WorkOrderResponse;
import com.example.maintenance.exception.*;
import com.example.maintenance.repository.*;
import com.example.maintenance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * The core maintenance workflow: create, assign, start, hold, complete and cancel work orders.
 * Every status change goes through {@link #transition} so the rules in {@link WorkOrderStatus}
 * and the audit trail in work_order_status_history are always applied together.
 */
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private static final List<WorkOrderStatus> ACTIVE_STATUSES = List.of(
            WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.ON_HOLD);

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final SparePartRepository sparePartRepository;
    private final UserRepository userRepository;
    private final ScheduleService scheduleService;

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Page<WorkOrderResponse> search(WorkOrderStatus status, WorkOrderPriority priority, WorkOrderType type,
                                          Long vesselId, Long equipmentId, Long assignedToId, boolean overdueOnly,
                                          String q, Pageable pageable) {
        Specification<WorkOrder> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (vesselId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("equipment").get("vessel").get("id"), vesselId));
        }
        if (equipmentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("equipment").get("id"), equipmentId));
        }
        if (assignedToId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), assignedToId));
        }
        if (overdueOnly) {
            LocalDate today = LocalDate.now();
            spec = spec.and((root, query, cb) -> cb.and(
                    root.get("status").in(ACTIVE_STATUSES),
                    cb.lessThan(root.<LocalDate>get("dueDate"), today)));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("workOrderNumber")), like)));
        }
        return workOrderRepository.findAll(spec, pageable).map(w -> WorkOrderResponse.from(w, false));
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse get(Long id) {
        return WorkOrderResponse.from(find(id), true);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderHistoryResponse> history(Long id) {
        if (!workOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Work order not found with id " + id);
        }
        return historyRepository.findByWorkOrderIdOrderByChangedAtAscIdAsc(id).stream()
                .map(WorkOrderHistoryResponse::from).toList();
    }

    // ------------------------------------------------------------------ creation

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest req, UserPrincipal principal) {
        Equipment equipment = equipmentRepository.findById(req.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id " + req.equipmentId()));
        MaintenanceTask task = null;
        if (req.taskId() != null) {
            task = taskRepository.findById(req.taskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + req.taskId()));
            if (!task.getEquipment().getId().equals(equipment.getId())) {
                throw new BadRequestException("The selected task belongs to different equipment");
            }
        }
        User assignee = null;
        if (req.assignedToId() != null) {
            requireManager(principal, "Only administrators and superintendents can assign work orders");
            assignee = findAssignee(req.assignedToId());
        }
        User creator = loadUser(principal);
        WorkOrder saved = persistNew(equipment, task, null, req.title().trim(), req.description(),
                req.type(), req.priority(), req.dueDate(), assignee, creator);
        return WorkOrderResponse.from(saved, true);
    }

    /** Turns a preventive maintenance schedule into a concrete work order. */
    @Transactional
    public WorkOrderResponse createFromSchedule(Long scheduleId, GenerateWorkOrderRequest req, UserPrincipal principal) {
        requireManager(principal, "Only administrators and superintendents can generate work orders");
        MaintenanceSchedule schedule = scheduleRepository.findByIdWithDetails(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + scheduleId));
        if (!schedule.isActive()) {
            throw new BadRequestException("This schedule is inactive");
        }
        if (workOrderRepository.existsByScheduleIdAndStatusIn(scheduleId, ACTIVE_STATUSES)) {
            throw new DuplicateResourceException("An open work order already exists for this schedule");
        }
        MaintenanceTask task = schedule.getTask();
        User assignee = req != null && req.assignedToId() != null ? findAssignee(req.assignedToId()) : null;
        WorkOrderPriority priority = req != null && req.priority() != null ? req.priority() : WorkOrderPriority.MEDIUM;

        WorkOrder saved = persistNew(schedule.getEquipment(), task, schedule, task.getTitle(), task.getDescription(),
                WorkOrderType.PREVENTIVE, priority, schedule.getNextDueDate(), assignee, loadUser(principal));
        return WorkOrderResponse.from(saved, true);
    }

    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderUpdateRequest req, UserPrincipal principal) {
        requireManager(principal, "Only administrators and superintendents can edit work orders");
        WorkOrder wo = find(id);
        if (wo.getStatus().isTerminal()) {
            throw new InvalidWorkOrderStateException("A " + wo.getStatus() + " work order can no longer be edited");
        }
        wo.setTitle(req.title().trim());
        wo.setDescription(req.description());
        wo.setPriority(req.priority());
        wo.setDueDate(req.dueDate());
        return WorkOrderResponse.from(workOrderRepository.save(wo), true);
    }

    // ------------------------------------------------------------------ workflow

    @Transactional
    public WorkOrderResponse assign(Long id, AssignRequest req, UserPrincipal principal) {
        requireManager(principal, "Only administrators and superintendents can assign work orders");
        WorkOrder wo = find(id);
        if (wo.getStatus().isTerminal()) {
            throw new InvalidWorkOrderStateException("A " + wo.getStatus() + " work order cannot be assigned");
        }
        User assignee = findAssignee(req.userId());
        User actor = loadUser(principal);
        wo.setAssignedTo(assignee);

        if (wo.getStatus() == WorkOrderStatus.OPEN) {
            transition(wo, WorkOrderStatus.ASSIGNED, actor, "Assigned to " + assignee.getFullName());
        } else {
            addHistory(wo, wo.getStatus(), wo.getStatus(), actor, "Reassigned to " + assignee.getFullName());
        }
        return WorkOrderResponse.from(workOrderRepository.save(wo), true);
    }

    @Transactional
    public WorkOrderResponse unassign(Long id, UserPrincipal principal) {
        requireManager(principal, "Only administrators and superintendents can unassign work orders");
        WorkOrder wo = find(id);
        User actor = loadUser(principal);
        transition(wo, WorkOrderStatus.OPEN, actor, "Unassigned");
        wo.setAssignedTo(null);
        return WorkOrderResponse.from(workOrderRepository.save(wo), true);
    }

    @Transactional
    public WorkOrderResponse start(Long id, UserPrincipal principal) {
        WorkOrder wo = find(id);
        requireCanWork(wo, principal);
        User actor = loadUser(principal);
        transition(wo, WorkOrderStatus.IN_PROGRESS, actor, "Work started");
        if (wo.getStartedAt() == null) {
            wo.setStartedAt(Instant.now());
        }
        return WorkOrderResponse.from(workOrderRepository.save(wo), true);
    }

    @Transactional
    public WorkOrderResponse hold(Long id, NotesRequest req, UserPrincipal principal) {
        WorkOrder wo = find(id);
        requireCanWork(wo, principal);
        transition(wo, WorkOrderStatus.ON_HOLD, loadUser(principal), req.notes());
        return WorkOrderResponse.from(workOrderRepository.save(wo), true);
    }

    @Transactional
    public WorkOrderResponse resume(Long id, UserPrincipal principal) {
        WorkOrder wo = find(id);
        requireCanWork(wo, principal);
        transition(wo, WorkOrderStatus.IN_PROGRESS, loadUser(principal), "Work resumed");
        return WorkOrderResponse.from(workOrderRepository.save(wo), true);
    }

    /**
     * Completing a work order closes the loop: it writes the permanent maintenance record, snapshots the
     * equipment's hour meters, and restarts the clock on the preventive schedule that generated it.
     */
    @Transactional
    public WorkOrderResponse complete(Long id, CompleteWorkOrderRequest req, UserPrincipal principal) {
        WorkOrder wo = find(id);
        requireCanWork(wo, principal);
        User actor = loadUser(principal);

        transition(wo, WorkOrderStatus.COMPLETED, actor, req.completionRemarks());
        Instant now = Instant.now();
        wo.setCompletedAt(now);
        wo.setCompletionRemarks(req.completionRemarks().trim());
        wo.setActualHours(req.actualHours());
        workOrderRepository.save(wo);

        Equipment equipment = wo.getEquipment();
        MaintenanceRecord record = MaintenanceRecord.builder()
                .workOrder(wo)
                .equipment(equipment)
                .performedBy(wo.getAssignedTo() != null ? wo.getAssignedTo() : actor)
                .completedAt(now)
                .runningHoursAtService(equipment.getRunningHours())
                .operatingHoursAtService(equipment.getOperatingHours())
                .actualHours(req.actualHours())
                .partsCost(wo.partsCost())
                .workPerformed(req.completionRemarks().trim())
                .build();
        recordRepository.save(record);

        if (wo.getSchedule() != null) {
            scheduleService.advanceAfterCompletion(wo.getSchedule(), LocalDate.now());
        }
        return WorkOrderResponse.from(wo, true);
    }

    @Transactional
    public WorkOrderResponse cancel(Long id, NotesRequest req, UserPrincipal principal) {
        requireManager(principal, "Only administrators and superintendents can cancel work orders");
        WorkOrder wo = find(id);
        transition(wo, WorkOrderStatus.CANCELLED, loadUser(principal), req.notes());
        return WorkOrderResponse.from(workOrderRepository.save(wo), true);
    }

    // ------------------------------------------------------------------ spare parts

    /** Draws spare parts from stock onto an in-progress work order. */
    @Transactional
    public WorkOrderResponse addPart(Long id, AddPartRequest req, UserPrincipal principal) {
        WorkOrder wo = find(id);
        requireCanWork(wo, principal);
        requireInProgress(wo);

        SparePart part = sparePartRepository.findById(req.sparePartId())
                .orElseThrow(() -> new ResourceNotFoundException("Spare part not found with id " + req.sparePartId()));
        if (part.getQuantityInStock() < req.quantity()) {
            throw new InsufficientStockException("Only " + part.getQuantityInStock() + " " + part.getUnit()
                    + " of " + part.getPartNumber() + " in stock; requested " + req.quantity());
        }
        part.setQuantityInStock(part.getQuantityInStock() - req.quantity());
        sparePartRepository.save(part);

        WorkOrderPart line = wo.getParts().stream()
                .filter(p -> p.getSparePart().getId().equals(part.getId()))
                .findFirst()
                .orElse(null);
        if (line != null) {
            line.setQuantityUsed(line.getQuantityUsed() + req.quantity());
        } else {
            wo.getParts().add(WorkOrderPart.builder()
                    .workOrder(wo)
                    .sparePart(part)
                    .quantityUsed(req.quantity())
                    .unitCostAtUse(part.getUnitCost())
                    .build());
        }
        return WorkOrderResponse.from(workOrderRepository.saveAndFlush(wo), true);
    }

    /** Returns a part line to stock (e.g. it was added by mistake). */
    @Transactional
    public WorkOrderResponse removePart(Long id, Long lineId, UserPrincipal principal) {
        WorkOrder wo = find(id);
        requireCanWork(wo, principal);
        requireInProgress(wo);

        WorkOrderPart line = wo.getParts().stream()
                .filter(p -> p.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Part line not found with id " + lineId));
        SparePart part = line.getSparePart();
        part.setQuantityInStock(part.getQuantityInStock() + line.getQuantityUsed());
        sparePartRepository.save(part);
        wo.getParts().remove(line);
        return WorkOrderResponse.from(workOrderRepository.saveAndFlush(wo), true);
    }

    // ------------------------------------------------------------------ helpers

    private WorkOrder persistNew(Equipment equipment, MaintenanceTask task, MaintenanceSchedule schedule,
                                 String title, String description, WorkOrderType type, WorkOrderPriority priority,
                                 LocalDate dueDate, User assignee, User creator) {
        if (equipment.getVessel().getStatus() == VesselStatus.DECOMMISSIONED) {
            throw new BadRequestException("Work orders cannot be raised for a decommissioned vessel");
        }
        WorkOrder wo = WorkOrder.builder()
                .workOrderNumber("PENDING-" + UUID.randomUUID())
                .equipment(equipment)
                .task(task)
                .schedule(schedule)
                .title(title)
                .description(description)
                .type(type)
                .priority(priority)
                .status(assignee == null ? WorkOrderStatus.OPEN : WorkOrderStatus.ASSIGNED)
                .createdBy(creator)
                .assignedTo(assignee)
                .dueDate(dueDate)
                .build();
        WorkOrder saved = workOrderRepository.saveAndFlush(wo);
        // The number is derived from the generated id, so it is unique without any counter table.
        saved.setWorkOrderNumber(String.format("WO-%d-%05d", Year.now().getValue(), saved.getId()));
        addHistory(saved, null, saved.getStatus(), creator,
                assignee == null ? "Work order created" : "Work order created and assigned to " + assignee.getFullName());
        return workOrderRepository.save(saved);
    }

    private void transition(WorkOrder wo, WorkOrderStatus next, User actor, String notes) {
        WorkOrderStatus from = wo.getStatus();
        if (!from.canTransitionTo(next)) {
            throw new InvalidWorkOrderStateException("A work order cannot move from " + from + " to " + next);
        }
        wo.setStatus(next);
        addHistory(wo, from, next, actor, notes);
    }

    private void addHistory(WorkOrder wo, WorkOrderStatus from, WorkOrderStatus to, User actor, String notes) {
        historyRepository.save(WorkOrderStatusHistory.builder()
                .workOrder(wo)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(actor)
                .notes(notes)
                .changedAt(Instant.now())
                .build());
    }

    private void requireInProgress(WorkOrder wo) {
        if (wo.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new InvalidWorkOrderStateException("Parts can only be changed while the work order is IN_PROGRESS");
        }
    }

    private void requireManager(UserPrincipal principal, String message) {
        if (!principal.isManager()) {
            throw new AccessDeniedException(message);
        }
    }

    /** Only the assigned engineer (or a manager) may progress a work order. */
    private void requireCanWork(WorkOrder wo, UserPrincipal principal) {
        boolean assignee = wo.getAssignedTo() != null && wo.getAssignedTo().getId().equals(principal.getId());
        if (!principal.isManager() && !assignee) {
            throw new AccessDeniedException("Only the assigned engineer or a superintendent can update this work order");
        }
    }

    private User findAssignee(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        if (!user.isEnabled()) {
            throw new BadRequestException("Cannot assign work to a disabled user");
        }
        return user;
    }

    private User loadUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + principal.getId()));
    }

    private WorkOrder find(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id " + id));
    }
}
