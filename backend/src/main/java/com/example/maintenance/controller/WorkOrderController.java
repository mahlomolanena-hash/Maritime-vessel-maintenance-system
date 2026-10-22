package com.example.maintenance.controller;

import com.example.maintenance.domain.WorkOrderPriority;
import com.example.maintenance.domain.WorkOrderStatus;
import com.example.maintenance.domain.WorkOrderType;
import com.example.maintenance.dto.request.*;
import com.example.maintenance.dto.response.PageResponse;
import com.example.maintenance.dto.response.WorkOrderHistoryResponse;
import com.example.maintenance.dto.response.WorkOrderResponse;
import com.example.maintenance.security.UserPrincipal;
import com.example.maintenance.service.WorkOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Role checks that depend on the work order itself (is this engineer the assignee?) live in
 * WorkOrderService; the controller only needs the authenticated principal.
 */
@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
@Tag(name = "Work Orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public ResponseEntity<PageResponse<WorkOrderResponse>> search(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) WorkOrderType type,
            @RequestParam(required = false) Long vesselId,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(workOrderService.search(status, priority, type, vesselId, equipmentId,
                assignedToId, overdue, q, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    /** The calling user's own active work. */
    @GetMapping("/mine")
    public ResponseEntity<PageResponse<WorkOrderResponse>> mine(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(PageResponse.from(workOrderService.search(status, null, null, null, null,
                principal.getId(), false, null, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.get(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<WorkOrderHistoryResponse>> history(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.history(id));
    }

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.create(request, principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> update(@PathVariable Long id, @Valid @RequestBody WorkOrderUpdateRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.update(id, request, principal));
    }

    // ---- workflow

    @PostMapping("/{id}/assign")
    public ResponseEntity<WorkOrderResponse> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.assign(id, request, principal));
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<WorkOrderResponse> unassign(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.unassign(id, principal));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<WorkOrderResponse> start(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.start(id, principal));
    }

    @PostMapping("/{id}/hold")
    public ResponseEntity<WorkOrderResponse> hold(@PathVariable Long id, @Valid @RequestBody NotesRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.hold(id, request, principal));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<WorkOrderResponse> resume(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.resume(id, principal));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<WorkOrderResponse> complete(@PathVariable Long id, @Valid @RequestBody CompleteWorkOrderRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.complete(id, request, principal));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<WorkOrderResponse> cancel(@PathVariable Long id, @Valid @RequestBody NotesRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.cancel(id, request, principal));
    }

    // ---- spare parts used on the job

    @PostMapping("/{id}/parts")
    public ResponseEntity<WorkOrderResponse> addPart(@PathVariable Long id, @Valid @RequestBody AddPartRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.addPart(id, request, principal));
    }

    @DeleteMapping("/{id}/parts/{lineId}")
    public ResponseEntity<WorkOrderResponse> removePart(@PathVariable Long id, @PathVariable Long lineId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.removePart(id, lineId, principal));
    }
}
