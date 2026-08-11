package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.ChallengeCompletionRequest;
import com.ecotrack.dto.ChallengeCompletionResponse;
import com.ecotrack.dto.ChallengeResponse;
import com.ecotrack.dto.LeaderboardResponse;
import com.ecotrack.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getDailyChallenges() {
        String authenticatedEmail = getAuthenticatedEmail();
        List<ChallengeResponse> challenges = challengeService.getDailyChallenges(authenticatedEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Daily challenges fetched successfully", challenges));
    }

    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getWeeklyChallenges() {
        String authenticatedEmail = getAuthenticatedEmail();
        List<ChallengeResponse> challenges = challengeService.getWeeklyChallenges(authenticatedEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Weekly challenges fetched successfully", challenges));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ChallengeCompletionResponse>> completeChallenge(@Valid @RequestBody ChallengeCompletionRequest request) {
        String authenticatedEmail = getAuthenticatedEmail();
        ChallengeCompletionResponse response = challengeService.completeChallenge(request, authenticatedEmail);
        return new ResponseEntity<>(new ApiResponse<>(true, "Challenge completed successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardResponse>>> getLeaderboard() {
        String authenticatedEmail = getAuthenticatedEmail();
        List<LeaderboardResponse> leaderboard = challengeService.getLeaderboard(authenticatedEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Leaderboard fetched successfully", leaderboard));
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}

