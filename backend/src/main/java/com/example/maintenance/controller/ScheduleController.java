package com.example.maintenance.controller;

import com.example.maintenance.domain.DueStatus;
import com.example.maintenance.dto.request.GenerateWorkOrderRequest;
import com.example.maintenance.dto.request.ScheduleRequest;
import com.example.maintenance.dto.response.ScheduleResponse;
import com.example.maintenance.dto.response.WorkOrderResponse;
import com.example.maintenance.security.Access;
import com.example.maintenance.security.UserPrincipal;
import com.example.maintenance.service.ScheduleService;
import com.example.maintenance.service.WorkOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Maintenance Schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final WorkOrderService workOrderService;

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> list(@RequestParam(required = false) Long equipmentId,
                                                       @RequestParam(required = false) Long vesselId,
                                                       @RequestParam(required = false) DueStatus dueStatus) {
        return ResponseEntity.ok(scheduleService.list(equipmentId, vesselId, dueStatus));
    }

    /** Overdue and due-soon schedules, most urgent first: the "what needs doing" view. */
    @GetMapping("/due")
    public ResponseEntity<List<ScheduleResponse>> due(@RequestParam(required = false) Long vesselId) {
        return ResponseEntity.ok(scheduleService.due(vesselId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.get(id));
    }

    @PostMapping
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<ScheduleResponse> update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Creates a preventive work order for this schedule. Body is optional (priority, assignee). */
    @PostMapping("/{id}/work-orders")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<WorkOrderResponse> generateWorkOrder(@PathVariable Long id,
                                                               @RequestBody(required = false) GenerateWorkOrderRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.createFromSchedule(id, request, principal));
    }
}
