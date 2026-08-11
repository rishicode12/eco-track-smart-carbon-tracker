package com.ecotrack.service.impl;

import com.ecotrack.dto.AIInsightResponse;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.Goal;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.GoalRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.AIRuleService;
import com.ecotrack.service.GenerativeAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerativeAIServiceImpl implements GenerativeAIService {

    private final ChatModel chatModel;
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final AIRuleService airRuleService;

    @Override
    public AIInsightResponse getInsights(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfileData profileData = buildUserProfileData(user);

        try {
            return generateWithGenerativeAI(profileData, user);
        } catch (Exception ex) {
            log.warn("Generative AI failed ({}), falling back to rule-based insights", ex.getMessage());
            return airRuleService.generateInsights(userEmail);
        }
    }

    private AIInsightResponse generateWithGenerativeAI(UserProfileData profileData, User user) {
        ChatClient chatClient = ChatClient.create(chatModel);

        String systemPrompt = buildSystemPrompt(profileData);

        AIInsightResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user("Based on the user's profile and carbon data provided above, generate the structured AI insight response in the exact JSON format specified.")
                .call()
                .entity(AIInsightResponse.class);

        log.debug("Generative AI response: {}", response);
        return response;
    }

    private UserProfileData buildUserProfileData(User user) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDate sevenDaysAgo = today.minusDays(7);
        LocalDate fourteenDaysAgo = today.minusDays(14);

        LocalDateTime thirtyStart = thirtyDaysAgo.atStartOfDay();
        LocalDateTime sevenStart = sevenDaysAgo.atStartOfDay();
        LocalDateTime fourteenStart = fourteenDaysAgo.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<CarbonEmission> last30 = carbonEmissionRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), thirtyStart, now);
        List<CarbonEmission> last7 = carbonEmissionRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), sevenStart, now);
        List<CarbonEmission> prev7 = carbonEmissionRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), fourteenStart, sevenStart);

        BigDecimal transportTotal = carbonEmissionRepository.sumTransportationEmission(user.getId(), thirtyStart);
        BigDecimal electricityTotal = carbonEmissionRepository.sumElectricityEmission(user.getId(), thirtyStart);
        BigDecimal foodTotal = carbonEmissionRepository.sumFoodEmission(user.getId(), thirtyStart);
        BigDecimal wasteTotal = carbonEmissionRepository.sumWasteEmission(user.getId(), thirtyStart);

        double total30 = last30.stream().mapToDouble(c -> c.getTotalEmission().doubleValue()).sum();
        double totalLast7 = last7.stream().mapToDouble(c -> c.getTotalEmission().doubleValue()).sum();
        double totalPrev7 = prev7.stream().mapToDouble(c -> c.getTotalEmission().doubleValue()).sum();

        List<Goal> activeGoals = goalRepository.findByUserEmailOrderByDeadlineAsc(user.getEmail())
                .stream().filter(g -> !Boolean.TRUE.equals(g.getIsCompleted())).toList();

        StringBuilder recentActivities = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (CarbonEmission c : last30.stream().limit(5).toList()) {
            recentActivities.append("- ")
                    .append(c.getCreatedAt().format(fmt))
                    .append(": ")
                    .append(c.getDescription() != null ? c.getDescription() : "Carbon activity")
                    .append(" | Total: ")
                    .append(c.getTotalEmission())
                    .append(" kg CO₂e\n");
        }

        return new UserProfileData(
                user.getFullName(),
                user.getCommuteMode() != null ? user.getCommuteMode() : "unknown",
                user.getDietPreference() != null ? user.getDietPreference() : "unknown",
                transportTotal.doubleValue(),
                electricityTotal.doubleValue(),
                foodTotal.doubleValue(),
                wasteTotal.doubleValue(),
                total30,
                totalLast7,
                totalPrev7,
                last30.size(),
                recentActivities.toString(),
                activeGoals.stream()
                        .map(g -> g.getTitle() + " (Target: " + g.getTargetCarbonReduction() + " kg, Deadline: " + g.getDeadline() + ")")
                        .collect(Collectors.joining("; ")),
                total30 > 0
                        ? String.format("Transport: %.1f%%, Energy: %.1f%%, Food: %.1f%%, Waste: %.1f%%",
                                (transportTotal.doubleValue() / total30) * 100,
                                (electricityTotal.doubleValue() / total30) * 100,
                                (foodTotal.doubleValue() / total30) * 100,
                                (wasteTotal.doubleValue() / total30) * 100)
                        : "No data"
        );
    }

    private String buildSystemPrompt(UserProfileData d) {
        return """
                You are an expert sustainability and carbon footprint advisor. Analyze the user's data and generate personalized, actionable insights.

                IMPORTANT: You must respond ONLY with a valid JSON object in the exact format specified. No additional text, no markdown code blocks, no explanations.

                Required JSON output format:
                {
                  "strengths": ["strength 1", "strength 2", ...],
                  "weaknesses": ["weakness 1", "weakness 2", ...],
                  "recommendations": ["recommendation 1", "recommendation 2", ...],
                  "priorityActions": ["priority action 1", "priority action 2", ...],
                  "riskLevel": "LOW" or "MEDIUM" or "HIGH",
                  "nextGoalSuggestion": "A specific, actionable goal suggestion string",
                  "predictedMonthlyCarbon": a number representing predicted monthly carbon in kg CO2e
                }

                Rules for generating insights:

                STRENGTHS:
                - Low-carbon commute (EV, transit) — highlight it
                - Plant-based diet — note the reduction contribution
                - Any category under 25%% of total — controlled emissions
                - Decreasing weekly trend — positive momentum

                WEAKNESSES:
                - Any category over 40%% of total — dominant emission source
                - Petrol sedan commute — high-carbon transport
                - Meat-heavy diet — high food emissions
                - Rising weekly trend — increasing emissions

                RECOMMENDATIONS — Actionable suggestions tied to user's specific data:
                - Commute-mode specific advice
                - Diet-specific advice
                - Category-specific efficiency tips
                - Trend-based warnings or encouragement

                PRIORITY ACTIONS — Only for dominant categories (over 40%%):
                - Specific immediate steps for the highest-emission category
                - Be concrete, not generic

                RISK LEVEL:
                - HIGH: Last 7 days emissions increased more than 25%% vs prior 7 days
                - MEDIUM: Last 7 days increased more than 15%% vs prior 7 days
                - LOW: Stable or decreasing trend, or no data

                NEXT GOAL SUGGESTION — Based on highest emission category:
                - Transport dominant — EV switch, carpooling, transit goals
                - Energy dominant — energy reduction targets
                - Food dominant — plant-based meal goals
                - Waste dominant — zero-waste targets

                PREDICTED MONTHLY CARBON — Extrapolate average daily from last 30 days x 30

                User Data:
                Name: %s
                Commute Mode: %s
                Diet Preference: %s
                Last 30 Days — Transport: %.2f kg CO2e | Energy: %.2f kg CO2e | Food: %.2f kg CO2e | Waste: %.2f kg CO2e
                Total Last 30 Days: %.2f kg CO2e
                Last 7 Days Total: %.2f kg CO2e | Prior 7 Days: %.2f kg CO2e
                Activity Count (30 days): %d entries
                Category Breakdown: %s
                Active Goals: %s
                Recent Activities:\n%s

                Respond ONLY with valid JSON matching the required format.
                """.formatted(
                d.fullName(), d.commuteMode(), d.dietPreference(),
                d.transportEmission(), d.electricityEmission(), d.foodEmission(), d.wasteEmission(),
                d.totalEmission(), d.last7Emission(), d.prev7Emission(),
                d.activityCount(), d.categoryBreakdown(), d.activeGoals(),
                d.recentActivities()
        );
    }

    private record UserProfileData(
            String fullName,
            String commuteMode,
            String dietPreference,
            double transportEmission,
            double electricityEmission,
            double foodEmission,
            double wasteEmission,
            double totalEmission,
            double last7Emission,
            double prev7Emission,
            int activityCount,
            String recentActivities,
            String activeGoals,
            String categoryBreakdown
    ) {}
}
