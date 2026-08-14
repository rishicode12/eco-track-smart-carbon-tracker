package com.ecotrack.dto;

import com.ecotrack.entity.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeResponse {
    private Long id;
    private String title;
    private String description;
    private ChallengeType challengeType;
    private Integer rewardPoints;
    private String badgeName;
    private Boolean active;
    private LocalDateTime createdAt;
    private Double targetGoal;
    private String metric;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Boolean isJoined;
    private Double currentProgress;
}

