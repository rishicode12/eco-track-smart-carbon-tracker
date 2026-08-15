package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcoLeaderboardResponse {
    private Integer rank;
    private Long userId;
    private String fullName;
    private Integer totalXp;
    private Integer currentLevel;
}