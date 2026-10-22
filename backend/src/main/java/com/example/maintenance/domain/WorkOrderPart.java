package com.example.maintenance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/** A spare part consumed by a work order; the unit cost is frozen at the time of use. */
@Entity
@Table(name = "work_order_parts",
        uniqueConstraints = @UniqueConstraint(name = "uk_wo_part", columnNames = {"work_order_id", "spare_part_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id", nullable = false)
    private SparePart sparePart;

    @Column(name = "quantity_used", nullable = false)
    private int quantityUsed;

    @Column(name = "unit_cost_at_use", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCostAtUse;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public BigDecimal lineCost() {
        return unitCostAtUse.multiply(BigDecimal.valueOf(quantityUsed));
    }
}
