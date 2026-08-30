package com.ecotrack.dto;

import com.ecotrack.entity.ChallengeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChallengeCreateRequest {

    @NotBlank(message = "Challenge title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @NotBlank(message = "Challenge description is required")
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotNull(message = "Challenge type is required")
    private ChallengeType challengeType;

    @NotNull(message = "Reward points are required")
    private Integer rewardPoints;

    @NotBlank(message = "Badge name is required")
    private String badgeName;

    private String category;

    private Double targetGoal;

    @Size(max = 100, message = "Metric must be at most 100 characters")
    private String metric;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;
}