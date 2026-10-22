package com.example.maintenance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Makes a task preventive: repeat it every N days, or every N running/operating hours.
 * Calendar schedules use {@code lastDoneDate/nextDueDate}; hour schedules use
 * {@code lastDoneHours/nextDueHours} measured against the equipment's hour meter.
 */
@Entity
@Table(name = "maintenance_schedules",
        indexes = @Index(name = "idx_schedule_task", columnList = "task_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private MaintenanceTask task;

    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false)
    private IntervalType intervalType;

    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue;

    @Column(name = "last_done_date")
    private LocalDate lastDoneDate;

    @Column(name = "last_done_hours", precision = 12, scale = 1)
    private BigDecimal lastDoneHours;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "next_due_hours", precision = 12, scale = 1)
    private BigDecimal nextDueHours;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Equipment getEquipment() {
        return task.getEquipment();
    }

    public MeterType meter() {
        return intervalType == IntervalType.OPERATING_HOURS ? MeterType.OPERATING : MeterType.RUNNING;
    }
}
