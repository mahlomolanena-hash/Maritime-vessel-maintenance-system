package com.example.maintenance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The permanent maintenance history entry written when a work order is completed.
 * Hour-meter values are snapshotted so the record stays true even after meters move on.
 */
@Entity
@Table(name = "maintenance_records", indexes = {
        @Index(name = "idx_record_equipment", columnList = "equipment_id"),
        @Index(name = "idx_record_completed", columnList = "completed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false, unique = true)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "running_hours_at_service", precision = 12, scale = 1)
    private BigDecimal runningHoursAtService;

    @Column(name = "operating_hours_at_service", precision = 12, scale = 1)
    private BigDecimal operatingHoursAtService;

    @Column(name = "actual_hours", precision = 8, scale = 1)
    private BigDecimal actualHours;

    @Column(name = "parts_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal partsCost = BigDecimal.ZERO;

    @Column(name = "work_performed", length = 2000)
    private String workPerformed;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
