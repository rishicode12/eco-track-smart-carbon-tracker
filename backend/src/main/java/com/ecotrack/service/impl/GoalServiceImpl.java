package com.ecotrack.service.impl;

import com.ecotrack.dto.GoalRequest;
import com.ecotrack.dto.GoalResponse;
import com.ecotrack.entity.Goal;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.GoalRepository;
import com.ecotrack.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private static final int BASELINE_LOOKBACK_DAYS = 30;

    private final GoalRepository goalRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;

    @Override
    @Transactional
    public GoalResponse createGoal(
            String email,
            GoalRequest request
    ) {

        Goal goal = Goal.builder()
                .userEmail(email)
                .title(request.getTitle().trim())
                .targetCarbonReduction(request.getTargetCarbonReduction())
                .deadline(request.getDeadline())
                .currentProgress(0.0)
                .isCompleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        Goal savedGoal = goalRepository.save(goal);

        GoalResponse response = mapToResponse(savedGoal);

        notifyGoalUpdated();

        return response;
    }

    @Override
    @Transactional
    public List<GoalResponse> getUserGoals(String email) {

        return goalRepository
                .findByUserEmailOrderByDeadlineAsc(email)
                .stream()
                .map(this::refreshProgressIfNeeded)
                .toList();
    }

    @Override
    @Transactional
    public GoalResponse getGoalById(
            Long id,
            String email
    ) {

        Goal goal = findOwnedGoal(id, email);

        return refreshProgressIfNeeded(goal);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(
            Long id,
            String email,
            GoalRequest request
    ) {

        Goal goal = findOwnedGoal(id, email);

        goal.setTitle(request.getTitle().trim());
        goal.setTargetCarbonReduction(
                request.getTargetCarbonReduction()
        );
        goal.setDeadline(request.getDeadline());

        if (Boolean.TRUE.equals(goal.getIsCompleted())) {
            goal.setIsCompleted(false);
        }

        Goal savedGoal = goalRepository.save(goal);

        GoalResponse response =
                refreshProgressIfNeeded(savedGoal);

        notifyGoalUpdated();

        return response;
    }

    @Override
    @Transactional
    public GoalResponse completeGoal(
            Long id,
            String email
    ) {

        Goal goal = findOwnedGoal(id, email);

        goal.setIsCompleted(true);

        goal.setCurrentProgress(
                goal.getTargetCarbonReduction()
        );

        Goal savedGoal = goalRepository.save(goal);

        GoalResponse response =
                mapToResponse(savedGoal);

        notifyGoalUpdated();

        return response;
    }

    @Override
    @Transactional
    public void deleteGoal(
            Long id,
            String email
    ) {

        Goal goal = findOwnedGoal(id, email);

        goalRepository.delete(goal);

        notifyGoalUpdated();
    }

    private Goal findOwnedGoal(
            Long id,
            String email
    ) {

        Goal goal = goalRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Goal not found"
                        )
                );

        if (
                email == null ||
                goal.getUserEmail() == null ||
                !goal.getUserEmail()
                        .equalsIgnoreCase(email)
        ) {
            throw new ResourceNotFoundException(
                    "Goal not found"
            );
        }

        return goal;
    }

    private GoalResponse refreshProgressIfNeeded(
            Goal goal
    ) {

        if (Boolean.TRUE.equals(goal.getIsCompleted())) {
            return mapToResponse(goal);
        }

        double calculatedProgress =
                calculateCurrentProgress(goal);

        double target =
                goal.getTargetCarbonReduction() != null
                        ? goal.getTargetCarbonReduction()
                        : 0.0;

        double cappedProgress =
                Math.min(
                        calculatedProgress,
                        target
                );

        boolean reachedTarget =
                target > 0 &&
                cappedProgress >= target;

        double oldProgress =
                goal.getCurrentProgress() != null
                        ? goal.getCurrentProgress()
                        : 0.0;

        boolean progressChanged =
                Math.abs(
                        cappedProgress - oldProgress
                ) > 0.001;

        if (progressChanged || reachedTarget) {

            goal.setCurrentProgress(
                    reachedTarget
                            ? target
                            : cappedProgress
            );

            if (reachedTarget) {
                goal.setIsCompleted(true);
            }

            goal = goalRepository.save(goal);
        }

        return mapToResponse(goal);
    }

    private double calculateCurrentProgress(
            Goal goal
    ) {

        LocalDateTime createdAt =
                goal.getCreatedAt();

        LocalDateTime now =
                LocalDateTime.now();

        if (createdAt == null) {
            return 0.0;
        }

        LocalDateTime baselineStart =
                createdAt.minusDays(
                        BASELINE_LOOKBACK_DAYS
                );

        BigDecimal baselineSum =
                carbonEmissionRepository
                        .calculateEmissionBetween(
                                getUserId(
                                        goal.getUserEmail()
                                ),
                                baselineStart,
                                createdAt
                        );

        long baselineDays =
                Math.max(
                        ChronoUnit.DAYS.between(
                                baselineStart.toLocalDate(),
                                createdAt.toLocalDate()
                        ),
                        1
                );

        double baselineDaily =
                (
                        baselineSum != null
                                ? baselineSum.doubleValue()
                                : 0.0
                ) / baselineDays;

        long daysSinceGoal =
                Math.max(
                        ChronoUnit.DAYS.between(
                                createdAt.toLocalDate(),
                                now.toLocalDate()
                        ) + 1,
                        1
                );

        double expectedWithoutChange =
                baselineDaily * daysSinceGoal;

        BigDecimal actualSum =
                carbonEmissionRepository
                        .calculateEmissionBetween(
                                getUserId(
                                        goal.getUserEmail()
                                ),
                                createdAt,
                                now
                        );

        double actual =
                actualSum != null
                        ? actualSum.doubleValue()
                        : 0.0;

        double saved =
                Math.max(
                        0.0,
                        expectedWithoutChange - actual
                );

        double target =
                goal.getTargetCarbonReduction() != null
                        ? goal.getTargetCarbonReduction()
                        : 0.0;

        return Math.min(saved, target);
    }

    private Long getUserId(
            String email
    ) {

        return carbonEmissionRepository
                .findUserIdByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private GoalResponse mapToResponse(
            Goal goal
    ) {

        double target =
                goal.getTargetCarbonReduction() != null &&
                goal.getTargetCarbonReduction() > 0
                        ? goal.getTargetCarbonReduction()
                        : 1.0;

        double progress =
                goal.getCurrentProgress() != null
                        ? goal.getCurrentProgress()
                        : 0.0;

        double percent =
                Math.min(
                        100.0,
                        (progress / target) * 100.0
                );

        return GoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .targetCarbonReduction(
                        goal.getTargetCarbonReduction()
                )
                .currentProgress(
                        goal.getCurrentProgress()
                )
                .deadline(goal.getDeadline())
                .isCompleted(goal.getIsCompleted())
                .createdAt(goal.getCreatedAt())
                .progressPercent(
                        Math.round(percent * 10.0) / 10.0
                )
                .build();
    }

    private void notifyGoalUpdated() {

        /*
         * Goal changes are picked up by Angular Dashboard.
         * This is intentionally outside the database layer.
         */
    }
}