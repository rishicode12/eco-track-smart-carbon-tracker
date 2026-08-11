package com.ecotrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "target_carbon_reduction", nullable = false)
    private Double targetCarbonReduction;

    @Column(name = "current_progress", nullable = false)
    private Double currentProgress;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (currentProgress == null) {
            currentProgress = 0.0;
        }

        if (isCompleted == null) {
            isCompleted = false;
        }
    }
}