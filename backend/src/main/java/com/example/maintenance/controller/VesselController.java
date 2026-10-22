package com.example.maintenance.controller;

import com.example.maintenance.domain.VesselStatus;
import com.example.maintenance.dto.request.VesselRequest;
import com.example.maintenance.dto.response.EquipmentResponse;
import com.example.maintenance.dto.response.PageResponse;
import com.example.maintenance.dto.response.VesselResponse;
import com.example.maintenance.security.Access;
import com.example.maintenance.service.EquipmentService;
import com.example.maintenance.service.VesselService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vessels")
@RequiredArgsConstructor
@Tag(name = "Vessels")
public class VesselController {

    private final VesselService vesselService;
    private final EquipmentService equipmentService;

    @GetMapping
    public ResponseEntity<PageResponse<VesselResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VesselStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
                vesselService.search(q, status, Paging.of(page, size, Sort.by("name")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VesselResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(vesselService.get(id));
    }

    /** All equipment on the vessel (flat; build the tree from parentId). */
    @GetMapping("/{id}/equipment")
    public ResponseEntity<List<EquipmentResponse>> equipment(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.listForVessel(id));
    }

    @PostMapping
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<VesselResponse> create(@Valid @RequestBody VesselRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vesselService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<VesselResponse> update(@PathVariable Long id, @Valid @RequestBody VesselRequest request) {
        return ResponseEntity.ok(vesselService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vesselService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
