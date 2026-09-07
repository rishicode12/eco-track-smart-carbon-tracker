package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadgeDto {
    private Long id;
    private Long userId;
    private String badgeName;
    private String badgeType;
    private String description;
    private LocalDateTime earnedDate;
}
