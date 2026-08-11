package com.ecotrack.service.impl;

import com.ecotrack.dto.DashboardResponse;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.Goal;
import com.ecotrack.entity.User;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.GoalRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl
        implements DashboardService {

    private final UserRepository userRepository;

    private final CarbonEmissionRepository
            carbonEmissionRepository;

    private final GoalRepository goalRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(
            Long userId
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        LocalDate today =
                LocalDate.now();

        LocalDateTime todayStart =
                today.atStartOfDay();

        LocalDateTime monthStart =
                today
                        .withDayOfMonth(1)
                        .atStartOfDay();

        BigDecimal todayCarbon =
                carbonEmissionRepository
                        .calculateEmissionSince(
                                userId,
                                todayStart
                        );

        BigDecimal monthlyCarbon =
                carbonEmissionRepository
                        .calculateEmissionSince(
                                userId,
                                monthStart
                        );

        if (todayCarbon == null) {
            todayCarbon = BigDecimal.ZERO;
        }

        if (monthlyCarbon == null) {
            monthlyCarbon = BigDecimal.ZERO;
        }

        List<CarbonEmission> recentEmissions =
                carbonEmissionRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                userId
                        )
                        .stream()
                        .limit(5)
                        .collect(Collectors.toList());

        List<DashboardResponse.ActivityDTO>
                activities =
                recentEmissions
                        .stream()
                        .map(this::mapToActivityDTO)
                        .collect(Collectors.toList());

        List<String> chartLabels =
                new ArrayList<>();

        List<BigDecimal> chartValues =
                new ArrayList<>();

        for (int i = 6; i >= 0; i--) {

            LocalDate date =
                    today.minusDays(i);

            chartLabels.add(
                    date.format(
                            DateTimeFormatter.ofPattern(
                                    "EEE"
                            )
                    )
            );

            LocalDateTime dayStart =
                    date.atStartOfDay();

            LocalDateTime dayEnd =
                    date
                            .plusDays(1)
                            .atStartOfDay();

            BigDecimal dayEmission =
                    carbonEmissionRepository
                            .calculateEmissionBetween(
                                    userId,
                                    dayStart,
                                    dayEnd
                            );

            chartValues.add(
                    dayEmission != null
                            ? dayEmission
                            : BigDecimal.ZERO
            );
        }

        DashboardResponse.ChartDataDTO chartData =
                DashboardResponse.ChartDataDTO.builder()
                        .labels(chartLabels)
                        .values(chartValues)
                        .period("Last 7 Days")
                        .build();

        Integer streak =
                calculateStreak(userId);

        Integer ecoScore =
                calculateEcoScore(
                        userId,
                        monthlyCarbon
                );

        Integer goalProgress =
                calculateGoalProgress(
                        user.getEmail()
                );

        return DashboardResponse.builder()
                .fullName(user.getFullName())
                .profilePicture(
                        user.getProfilePicture()
                )
                .rewardPoints(
                        user.getRewardPoints()
                )
                .badgeName(
                        user.getBadgeName()
                )
                .todayCarbon(todayCarbon)
                .monthlyCarbon(monthlyCarbon)
                .streak(streak)
                .goalProgress(goalProgress)
                .ecoScore(ecoScore)
                .recentActivities(activities)
                .chartData(chartData)
                .build();
    }

    private DashboardResponse.ActivityDTO
    mapToActivityDTO(
            CarbonEmission emission
    ) {

        String type = "CARBON_LOGGED";

        String description =
                "Logged carbon emission: "
                        + emission
                        .getTotalEmission()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        + " kg CO2";

        String icon =
                "bi-calculator";

        if (
                emission
                        .getTransportationEmission()
                        .compareTo(
                                BigDecimal.ZERO
                        ) > 0
        ) {

            type = "TRANSPORT";

            description =
                    "Transport carbon logged: "
                            + emission
                            .getTransportationEmission()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            + " kg CO2";

            icon =
                    "bi-car-front";

        } else if (
                emission
                        .getElectricityEmission()
                        .compareTo(
                                BigDecimal.ZERO
                        ) > 0
        ) {

            type = "ENERGY";

            description =
                    "Energy usage logged: "
                            + emission
                            .getElectricityEmission()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            + " kg CO2";

            icon =
                    "bi-lightning-charge";

        } else if (
                emission
                        .getFoodEmission()
                        .compareTo(
                                BigDecimal.ZERO
                        ) > 0
        ) {

            type = "FOOD";

            description =
                    "Food carbon logged: "
                            + emission
                            .getFoodEmission()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            + " kg CO2";

            icon =
                    "bi-egg-fried";

        } else if (
                emission
                        .getWasteEmission()
                        .compareTo(
                                BigDecimal.ZERO
                        ) > 0
        ) {

            type = "WASTE";

            description =
                    "Waste carbon logged: "
                            + emission
                            .getWasteEmission()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            + " kg CO2";

            icon =
                    "bi-trash3";
        }

        return DashboardResponse.ActivityDTO
                .builder()
                .id(
                        String.valueOf(
                                emission.getId()
                        )
                )
                .type(type)
                .description(description)
                .impact(
                        emission.getTotalEmission()
                )
                .timestamp(
                        emission
                                .getCreatedAt()
                                .toString()
                )
                .icon(icon)
                .build();
    }

    private Integer calculateStreak(
            Long userId
    ) {

        List<CarbonEmission> emissions =
                carbonEmissionRepository
                        .findByUserId(userId);

        if (emissions.isEmpty()) {
            return 0;
        }

        int streak = 0;

        LocalDate today =
                LocalDate.now();

        for (int i = 0; i < 30; i++) {

            LocalDate checkDate =
                    today.minusDays(i);

            boolean hasActivity =
                    emissions
                            .stream()
                            .anyMatch(
                                    emission ->
                                            emission
                                                    .getCreatedAt()
                                                    .toLocalDate()
                                                    .equals(
                                                            checkDate
                                                    )
                            );

            if (hasActivity) {

                streak++;

            } else if (i == 0) {

                continue;

            } else {

                break;
            }
        }

        return streak;
    }

    private Integer calculateEcoScore(
            Long userId,
            BigDecimal monthlyCarbon
    ) {

        List<CarbonEmission> emissions =
                carbonEmissionRepository
                        .findByUserId(userId);

        if (emissions.isEmpty()) {
            return 50;
        }

        BigDecimal total =
                emissions
                        .stream()
                        .map(
                                CarbonEmission
                                        ::getTotalEmission
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal average =
                total.divide(
                        BigDecimal.valueOf(
                                emissions.size()
                        ),
                        2,
                        RoundingMode.HALF_UP
                );

        int score = 100;

        if (
                monthlyCarbon.compareTo(
                        average
                ) < 0
        ) {

            score += 20;

        } else if (
                monthlyCarbon.compareTo(
                        average.multiply(
                                BigDecimal.valueOf(1.5)
                        )
                ) > 0
        ) {

            score -= 20;
        }

        int activityBonus =
                Math.min(
                        emissions.size() * 2,
                        20
                );

        score += activityBonus;

        return Math.max(
                0,
                Math.min(100, score)
        );
    }

    private Integer calculateGoalProgress(
            String userEmail
    ) {

        List<Goal> goals =
                goalRepository
                        .findByUserEmailOrderByDeadlineAsc(
                                userEmail
                        );

        List<Goal> activeGoals =
                goals
                        .stream()
                        .filter(
                                goal ->
                                        !Boolean.TRUE.equals(
                                                goal.getIsCompleted()
                                        )
                        )
                        .toList();

        if (activeGoals.isEmpty()) {
            return 0;
        }

        double totalProgress =
                activeGoals
                        .stream()
                        .mapToDouble(
                                goal -> {

                                    double target =
                                            goal.getTargetCarbonReduction() != null
                                                    ? goal.getTargetCarbonReduction()
                                                    : 0.0;

                                    double progress =
                                            goal.getCurrentProgress() != null
                                                    ? goal.getCurrentProgress()
                                                    : 0.0;

                                    if (target <= 0) {
                                        return 0.0;
                                    }

                                    return Math.min(
                                            100.0,
                                            Math.max(
                                                    0.0,
                                                    (
                                                            progress /
                                                            target
                                                    ) * 100.0
                                            )
                                    );
                                }
                        )
                        .sum();

        return (int) Math.round(
                totalProgress /
                activeGoals.size()
        );
    }
}