package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.GoalRequest;
import com.ecotrack.dto.GoalResponse;
import com.ecotrack.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(
            @Valid @RequestBody GoalRequest request
    ) {

        String email = getAuthenticatedEmail();

        GoalResponse response =
                goalService.createGoal(
                        email,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(
                                true,
                                "Goal created successfully",
                                response
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getUserGoals() {

        String email = getAuthenticatedEmail();

        List<GoalResponse> goals =
                goalService.getUserGoals(email);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Goals fetched successfully",
                        goals
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoalById(
            @PathVariable Long id
    ) {

        String email = getAuthenticatedEmail();

        GoalResponse response =
                goalService.getGoalById(
                        id,
                        email
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Goal fetched successfully",
                        response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request
    ) {

        String email = getAuthenticatedEmail();

        GoalResponse response =
                goalService.updateGoal(
                        id,
                        email,
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Goal updated successfully",
                        response
                )
        );
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<GoalResponse>> completeGoal(
            @PathVariable Long id
    ) {

        String email = getAuthenticatedEmail();

        GoalResponse response =
                goalService.completeGoal(
                        id,
                        email
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Goal marked as complete",
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @PathVariable Long id
    ) {

        String email = getAuthenticatedEmail();

        goalService.deleteGoal(
                id,
                email
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Goal deleted successfully",
                        null
                )
        );
    }

    private String getAuthenticatedEmail() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return authentication != null
                ? authentication.getName()
                : null;
    }
}