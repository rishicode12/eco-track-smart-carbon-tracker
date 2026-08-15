package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcoProfileResponse {
    private Long userId;
    private Integer totalXp;
    private Integer currentLevel;
    private Set<String> unlockedBadges;
}