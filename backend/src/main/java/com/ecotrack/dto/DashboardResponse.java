package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private String fullName;
    private String profilePicture;
    private Integer rewardPoints;
    private String badgeName;

    private BigDecimal todayCarbon;
    private BigDecimal monthlyCarbon;
    private Integer streak;
    private Integer goalProgress;
    private Integer ecoScore;

    private List<ActivityDTO> recentActivities;
    private ChartDataDTO chartData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDTO {
        private String id;
        private String type;
        private String description;
        private BigDecimal impact;
        private String timestamp;
        private String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataDTO {
        private List<String> labels;
        private List<BigDecimal> values;
        private String period;
    }
}
