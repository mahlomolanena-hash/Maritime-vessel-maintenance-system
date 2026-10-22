package com.example.maintenance.controller;

import com.example.maintenance.domain.Criticality;
import com.example.maintenance.domain.DueStatus;
import com.example.maintenance.domain.EquipmentStatus;
import com.example.maintenance.dto.request.EquipmentRequest;
import com.example.maintenance.dto.request.HourReadingRequest;
import com.example.maintenance.dto.response.*;
import com.example.maintenance.security.Access;
import com.example.maintenance.security.UserPrincipal;
import com.example.maintenance.service.EquipmentService;
import com.example.maintenance.service.MaintenanceRecordService;
import com.example.maintenance.service.ScheduleService;
import com.example.maintenance.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final TaskService taskService;
    private final ScheduleService scheduleService;
    private final MaintenanceRecordService recordService;

    @GetMapping
    public ResponseEntity<PageResponse<EquipmentResponse>> search(
            @RequestParam(required = false) Long vesselId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) Criticality criticality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
                equipmentService.search(vesselId, q, status, criticality, Paging.of(page, size, Sort.by("name")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.get(id));
    }

    @PostMapping
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<EquipmentResponse> update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- hour meters (any authenticated crew member can log a reading)

    @GetMapping("/{id}/hours")
    public ResponseEntity<PageResponse<HourReadingResponse>> readings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
                equipmentService.readings(id, Paging.of(page, size, Sort.unsorted()))));
    }

    @PostMapping("/{id}/hours")
    public ResponseEntity<HourReadingResponse> recordReading(@PathVariable Long id,
                                                             @Valid @RequestBody HourReadingRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.recordReading(id, request, principal));
    }

    // ---- everything attached to one piece of equipment

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> tasks(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.list(id));
    }

    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<ScheduleResponse>> schedules(@PathVariable Long id,
                                                            @RequestParam(required = false) DueStatus dueStatus) {
        return ResponseEntity.ok(scheduleService.list(id, null, dueStatus));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<PageResponse<MaintenanceRecordResponse>> history(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(recordService.search(null, id, null, null, null, null,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt")))));
    }
}
