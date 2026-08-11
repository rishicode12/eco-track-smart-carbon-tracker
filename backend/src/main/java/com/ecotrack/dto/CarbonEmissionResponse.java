package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonEmissionResponse {

    private Long id;

    private String activityCategory;

    private BigDecimal co2Impact;

    private String description;

    private LocalDateTime loggedAt;
}