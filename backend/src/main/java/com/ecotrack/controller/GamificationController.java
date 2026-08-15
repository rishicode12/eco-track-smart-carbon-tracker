package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.EcoLeaderboardResponse;
import com.ecotrack.dto.EcoProfileResponse;
import com.ecotrack.service.EcoScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final EcoScoreService ecoScoreService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<EcoProfileResponse>> getProfile() {
        String authenticatedEmail = getAuthenticatedEmail();
        EcoProfileResponse profile = ecoScoreService.getProfile(authenticatedEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Eco profile fetched successfully", profile));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<EcoLeaderboardResponse>>> getLeaderboard() {
        List<EcoLeaderboardResponse> leaderboard = ecoScoreService.getLeaderboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Leaderboard fetched successfully", leaderboard));
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}