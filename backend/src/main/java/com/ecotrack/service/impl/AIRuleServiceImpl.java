package com.ecotrack.service.impl;

import com.ecotrack.dto.AIInsightResponse;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.AIRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AIRuleServiceImpl implements AIRuleService {

    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserRepository userRepository;

    @Override
    public AIInsightResponse generateInsights(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDate fourteenDaysAgo = today.minusDays(14);
        LocalDate sevenDaysAgo = today.minusDays(7);

        LocalDateTime thirtyDaysStart = thirtyDaysAgo.atStartOfDay();
        LocalDateTime fourteenDaysStart = fourteenDaysAgo.atStartOfDay();
        LocalDateTime sevenDaysStart = sevenDaysAgo.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<CarbonEmission> last30Days = carbonEmissionRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), thirtyDaysStart, now);

        List<CarbonEmission> last7Days = carbonEmissionRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), sevenDaysStart, now);

        List<CarbonEmission> prev7Days = carbonEmissionRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), fourteenDaysStart, sevenDaysStart);

        // Category sums for last 30 days
        BigDecimal transport = carbonEmissionRepository.sumTransportationEmission(user.getId(), thirtyDaysStart);
        BigDecimal electricity = carbonEmissionRepository.sumElectricityEmission(user.getId(), thirtyDaysStart);
        BigDecimal food = carbonEmissionRepository.sumFoodEmission(user.getId(), thirtyDaysStart);
        BigDecimal waste = carbonEmissionRepository.sumWasteEmission(user.getId(), thirtyDaysStart);

        double t = transport.doubleValue();
        double e = electricity.doubleValue();
        double f = food.doubleValue();
        double w = waste.doubleValue();
        double total = t + e + f + w;

        // Weekly comparison
        double last7Total = last7Days.stream().mapToDouble(c -> c.getTotalEmission().doubleValue()).sum();
        double prev7Total = prev7Days.stream().mapToDouble(c -> c.getTotalEmission().doubleValue()).sum();

        // Predicted monthly carbon
        double avgDaily = total / 30.0;
        double predictedMonthly = avgDaily * 30.0;

        // Risk level
        String riskLevel = determineRiskLevel(last7Total, prev7Total, total);

        // Strengths
        List<String> strengths = buildStrengths(user, t, e, f, w, total, last7Total, prev7Total);

        // Weaknesses
        List<String> weaknesses = buildWeaknesses(user, t, e, f, w, total);

        // Recommendations
        List<String> recommendations = buildRecommendations(user, t, e, f, w, total, last7Total, prev7Total);

        // Priority actions
        List<String> priorityActions = buildPriorityActions(t, e, f, w, total);

        // Next goal suggestion
        String nextGoalSuggestion = suggestGoal(t, e, f, w);

        return new AIInsightResponse(
                strengths,
                weaknesses,
                recommendations,
                priorityActions,
                riskLevel,
                nextGoalSuggestion,
                Math.round(predictedMonthly * 100.0) / 100.0
        );
    }

    private String determineRiskLevel(double last7, double prev7, double total30) {
        if (total30 == 0) return "LOW";

        double increase = ((last7 - prev7) / prev7) * 100;

        if (prev7 == 0 && last7 > 0) return "MEDIUM";
        if (increase > 25) return "HIGH";
        if (increase > 15) return "MEDIUM";
        return "LOW";
    }

    private List<String> buildStrengths(User user, double t, double e, double f, double w, double total, double last7, double prev7) {
        List<String> strengths = new ArrayList<>();

        if ("ev".equalsIgnoreCase(user.getCommuteMode()) || "transit".equalsIgnoreCase(user.getCommuteMode())) {
            strengths.add("Uses low-carbon commute mode: " + formatCommute(user.getCommuteMode()));
        }

        if ("vegan".equalsIgnoreCase(user.getDietPreference()) || "vegetarian".equalsIgnoreCase(user.getDietPreference())) {
            strengths.add("Plant-based diet significantly reduces food-related emissions");
        }

        if (total > 0) {
            if (t / total < 0.25) {
                strengths.add("Transportation emissions are well-controlled at " + pct(t, total) + "% of total footprint");
            }
            if (e / total < 0.25) {
                strengths.add("Energy consumption is efficient at " + pct(e, total) + "% of total footprint");
            }
            if (w / total < 0.15) {
                strengths.add("Waste management is minimal at " + pct(w, total) + "% of total footprint");
            }
        }

        double trend = prev7 > 0 ? ((last7 - prev7) / prev7) * 100 : 0;
        if (trend < 0) {
            strengths.add("Carbon footprint is trending downward (" + String.format("%.1f", Math.abs(trend)) + "% reduction in last 7 days vs prior week)");
        }

        if (strengths.isEmpty()) {
            strengths.add("Consistently logging carbon activities — data tracking is the first step to improvement");
        }

        return strengths;
    }

    private List<String> buildWeaknesses(User user, double t, double e, double f, double w, double total) {
        List<String> weaknesses = new ArrayList<>();

        if (total == 0) {
            weaknesses.add("No carbon activities logged in the last 30 days — start tracking to receive personalized insights");
            return weaknesses;
        }

        if (t / total > 0.40) {
            weaknesses.add("Transportation is your dominant emission source at " + pct(t, total) + "% of total footprint");
        }
        if (e / total > 0.40) {
            weaknesses.add("Energy consumption is the largest contributor at " + pct(e, total) + "% of total footprint");
        }
        if (f / total > 0.40) {
            weaknesses.add("Food choices account for " + pct(f, total) + "% of total emissions — high-carbon diet detected");
        }
        if (w / total > 0.25) {
            weaknesses.add("Waste emissions are elevated at " + pct(w, total) + "% of total footprint");
        }

        if ("sedan".equalsIgnoreCase(user.getCommuteMode())) {
            weaknesses.add("Petrol sedan/SUV is a high-carbon commute option — consider EV or public transit");
        }
        if ("meat".equalsIgnoreCase(user.getDietPreference())) {
            weaknesses.add("Meat-heavy diet has a significant carbon footprint — consider reducing meat consumption");
        }

        return weaknesses;
    }

    private List<String> buildRecommendations(User user, double t, double e, double f, double w, double total,
                                             double last7, double prev7) {
        List<String> recs = new ArrayList<>();

        if (total == 0) {
            recs.add("Start logging your daily carbon activities (transport, electricity, food, waste) to receive AI-powered insights");
            return recs;
        }

        if ("sedan".equalsIgnoreCase(user.getCommuteMode())) {
            recs.add("Switch to electric vehicle or public transit to cut transportation emissions by up to 50%");
        }

        if ("meat".equalsIgnoreCase(user.getDietPreference())) {
            recs.add("Reducing meat intake even 2-3 days a week can cut food-related emissions by 20-30%");
        }

        if (e / total > 0.30) {
            recs.add("Install LED lighting and energy-efficient appliances to reduce electricity consumption");
        }

        if (w / total > 0.15) {
            recs.add("Start composting organic waste and reduce single-use plastics to lower waste emissions");
        }

        double trend = prev7 > 0 ? ((last7 - prev7) / prev7) * 100 : 0;
        if (trend > 10 && prev7 > 0) {
            recs.add("Your carbon footprint increased by " + String.format("%.1f", trend) + "% last week vs the prior week — review recent activity changes");
        }

        if (t / total > 0.35) {
            recs.add("Consider carpooling, biking, or working from home to reduce high transportation emissions");
        }

        if (recs.isEmpty()) {
            recs.add("Continue maintaining your current low-carbon lifestyle and explore further reductions in energy usage");
        }

        return recs;
    }

    private List<String> buildPriorityActions(double t, double e, double f, double w, double total) {
        List<String> actions = new ArrayList<>();

        if (total == 0) return actions;

        if (t / total > 0.40) {
            actions.add("Immediately: Switch to green transportation (EV, public transit, cycling) to reduce highest emission source");
        }
        if (e / total > 0.40) {
            actions.add("Immediately: Audit home energy usage — unplug devices, use smart power strips, reduce HVAC load");
        }
        if (f / total > 0.40) {
            actions.add("Immediately: Cut food waste by 50% and replace one meat meal per day with plant-based alternatives");
        }
        if (w / total > 0.25) {
            actions.add("Immediately: Reduce waste output — use reusable containers, start composting, avoid single-use packaging");
        }

        return actions;
    }

    private String suggestGoal(double t, double e, double f, double w) {
        if (t >= e && t >= f && t >= w) {
            return "Reduce transportation emissions by 20% this month — try carpooling, public transit, or cycling for short distances";
        }
        if (e >= t && e >= f && e >= w) {
            return "Cut energy consumption by 15% — switch to LED bulbs, unplug idle devices, and optimize HVAC usage";
        }
        if (f >= t && f >= e && f >= w) {
            return "Adopt a plant-based diet for 3 days a week to reduce food-related carbon footprint by up to 25%";
        }
        if (w >= t && w >= e && w >= f) {
            return "Achieve zero-waste week — compost, recycle, and avoid single-use products for 7 days";
        }
        return "Set a goal to log daily carbon activities for the next 30 days to establish consistent tracking habits";
    }

    private String formatCommute(String mode) {
        return switch (mode.toLowerCase()) {
            case "ev" -> "Electric Vehicle";
            case "transit" -> "Public Transit";
            case "sedan" -> "Petrol Sedan/SUV";
            default -> mode;
        };
    }

    private String pct(double value, double total) {
        return String.format("%.1f", (value / total) * 100);
    }
}
