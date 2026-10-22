package com.example.maintenance.controller;

import com.example.maintenance.dto.request.SparePartRequest;
import com.example.maintenance.dto.request.StockAdjustRequest;
import com.example.maintenance.dto.response.PageResponse;
import com.example.maintenance.dto.response.SparePartResponse;
import com.example.maintenance.security.Access;
import com.example.maintenance.service.SparePartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/spare-parts")
@RequiredArgsConstructor
@Tag(name = "Spare Parts")
public class SparePartController {

    private final SparePartService sparePartService;

    @GetMapping
    public ResponseEntity<PageResponse<SparePartResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
                sparePartService.search(q, lowStock, Paging.of(page, size, Sort.by("name")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SparePartResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(sparePartService.get(id));
    }

    @PostMapping
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<SparePartResponse> create(@Valid @RequestBody SparePartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sparePartService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<SparePartResponse> update(@PathVariable Long id, @Valid @RequestBody SparePartRequest request) {
        return ResponseEntity.ok(sparePartService.update(id, request));
    }

    /** Receive stock (positive delta) or write off stock (negative delta). */
    @PatchMapping("/{id}/stock")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<SparePartResponse> adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest request) {
        return ResponseEntity.ok(sparePartService.adjustStock(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Access.MANAGER)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sparePartService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
