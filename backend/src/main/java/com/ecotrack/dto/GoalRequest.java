package com.ecotrack.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequest {

    @NotBlank(message = "Goal title is required")
    @Size(max = 150, message = "Goal title must not exceed 150 characters")
    private String title;

    @NotNull(message = "Target carbon reduction is required")
    @Positive(message = "Target carbon reduction must be greater than zero")
    private Double targetCarbonReduction;

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline must be today or a future date")
    private LocalDate deadline;
}