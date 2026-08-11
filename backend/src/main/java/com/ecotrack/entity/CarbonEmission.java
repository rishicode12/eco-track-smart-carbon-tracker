package com.ecotrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carbon_emissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarbonEmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transportation_emission", nullable = false)
    @Builder.Default
    private BigDecimal transportationEmission = BigDecimal.ZERO;

    @Column(name = "electricity_emission", nullable = false)
    @Builder.Default
    private BigDecimal electricityEmission = BigDecimal.ZERO;

    @Column(name = "food_emission", nullable = false)
    @Builder.Default
    private BigDecimal foodEmission = BigDecimal.ZERO;

    @Column(name = "waste_emission", nullable = false)
    @Builder.Default
    private BigDecimal wasteEmission = BigDecimal.ZERO;

    @Column(name = "total_emission", nullable = false)
    @Builder.Default
    private BigDecimal totalEmission = BigDecimal.ZERO;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "activity_category", length = 100)
    private String activityCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        normalizeValues();
        calculateTotalEmission();
    }

    @PreUpdate
    public void preUpdate() {
        normalizeValues();
        calculateTotalEmission();
    }

    private void normalizeValues() {
        if (transportationEmission == null) {
            transportationEmission = BigDecimal.ZERO;
        }

        if (electricityEmission == null) {
            electricityEmission = BigDecimal.ZERO;
        }

        if (foodEmission == null) {
            foodEmission = BigDecimal.ZERO;
        }

        if (wasteEmission == null) {
            wasteEmission = BigDecimal.ZERO;
        }
    }

    public void calculateTotalEmission() {
        normalizeValues();

        totalEmission = transportationEmission
                .add(electricityEmission)
                .add(foodEmission)
                .add(wasteEmission);
    }
}