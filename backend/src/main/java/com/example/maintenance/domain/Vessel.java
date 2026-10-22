package com.example.maintenance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vessels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vessel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "imo_number", nullable = false, unique = true, length = 7)
    private String imoNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vessel_type", nullable = false)
    private VesselType vesselType;

    private String flag;

    @Column(name = "year_built")
    private Integer yearBuilt;

    @Column(name = "gross_tonnage", precision = 12, scale = 2)
    private BigDecimal grossTonnage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VesselStatus status = VesselStatus.ACTIVE;

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
}
