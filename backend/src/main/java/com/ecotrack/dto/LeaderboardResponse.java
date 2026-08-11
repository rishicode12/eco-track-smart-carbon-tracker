package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private Integer rank;
    private Long userId;
    private String fullName;
    private Integer rewardPoints;
    private String badgeName;
}

