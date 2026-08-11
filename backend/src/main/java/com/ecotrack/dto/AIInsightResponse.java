package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIInsightResponse {
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private List<String> priorityActions;
    private String riskLevel;
    private String nextGoalSuggestion;
    private Double predictedMonthlyCarbon;
}
