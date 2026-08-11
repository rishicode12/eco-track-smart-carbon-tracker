package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeCompletionResponse {
    private Long challengeId;
    private String challengeTitle;
    private Integer rewardPointsEarned;
    private Integer totalRewardPoints;
    private String badgeEarned;
    private String message;
}

