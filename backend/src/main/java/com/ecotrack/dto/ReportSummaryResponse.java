package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportSummaryResponse {
    private Double todayCarbon;
    private Double weeklyCarbon;
    private Double monthlyCarbon;
    private Double yearlyCarbon;
    private Double avgDailyEmission;
    private Double momReductionPercent;
}
