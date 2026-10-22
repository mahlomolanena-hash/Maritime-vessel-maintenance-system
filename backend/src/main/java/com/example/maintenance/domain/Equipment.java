package com.example.maintenance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A piece of onboard machinery. Equipment can be nested through {@code parent}
 * (e.g. a main engine with its cooling pump) and carries the two hour meters that
 * drive hour-based maintenance schedules.
 */
@Entity
@Table(name = "equipment",
        uniqueConstraints = @UniqueConstraint(name = "uk_equipment_vessel_tag", columnNames = {"vessel_id", "tag_number"}),
        indexes = @Index(name = "idx_equipment_vessel", columnList = "vessel_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vessel_id", nullable = false)
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Equipment parent;

    @Column(nullable = false)
    private String name;

    @Column(name = "tag_number", nullable = false)
    private String tagNumber;

    private String category;

    private String manufacturer;

    private String model;

    @Column(name = "serial_number")
    private String serialNumber;

    private String location;

    @Column(name = "installed_date")
    private LocalDate installedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EquipmentStatus status = EquipmentStatus.OPERATIONAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Criticality criticality = Criticality.MEDIUM;

    @Column(name = "running_hours", nullable = false, precision = 12, scale = 1)
    @Builder.Default
    private BigDecimal runningHours = BigDecimal.ZERO;

    @Column(name = "operating_hours", nullable = false, precision = 12, scale = 1)
    @Builder.Default
    private BigDecimal operatingHours = BigDecimal.ZERO;

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

    public BigDecimal hoursFor(MeterType meter) {
        return meter == MeterType.RUNNING ? runningHours : operatingHours;
    }

    public void setHoursFor(MeterType meter, BigDecimal hours) {
        if (meter == MeterType.RUNNING) {
            this.runningHours = hours;
        } else {
            this.operatingHours = hours;
        }
    }
}
