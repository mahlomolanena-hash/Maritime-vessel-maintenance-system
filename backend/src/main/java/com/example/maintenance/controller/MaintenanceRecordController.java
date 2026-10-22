package com.example.maintenance.controller;

import com.example.maintenance.domain.WorkOrderType;
import com.example.maintenance.dto.response.MaintenanceRecordResponse;
import com.example.maintenance.dto.response.PageResponse;
import com.example.maintenance.service.MaintenanceRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/maintenance-records")
@RequiredArgsConstructor
@Tag(name = "Maintenance History")
public class MaintenanceRecordController {

    private final MaintenanceRecordService recordService;

    @GetMapping
    public ResponseEntity<PageResponse<MaintenanceRecordResponse>> search(
            @RequestParam(required = false) Long vesselId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) WorkOrderType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(recordService.search(vesselId, equipmentId, type, from, to, q,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRecordResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.get(id));
    }
}
