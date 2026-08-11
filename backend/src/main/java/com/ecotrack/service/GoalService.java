package com.ecotrack.service;

import com.ecotrack.dto.GoalRequest;
import com.ecotrack.dto.GoalResponse;

import java.util.List;

public interface GoalService {

    GoalResponse createGoal(
            String email,
            GoalRequest request
    );

    List<GoalResponse> getUserGoals(
            String email
    );

    GoalResponse getGoalById(
            Long id,
            String email
    );

    GoalResponse updateGoal(
            Long id,
            String email,
            GoalRequest request
    );

    GoalResponse completeGoal(
            Long id,
            String email
    );

    void deleteGoal(
            Long id,
            String email
    );
}