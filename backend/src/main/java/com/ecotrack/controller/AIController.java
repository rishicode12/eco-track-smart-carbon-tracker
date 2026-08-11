package com.ecotrack.controller;

import com.ecotrack.dto.AIInsightResponse;
import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.RecommendationResponse;
import com.ecotrack.service.AIService;
import com.ecotrack.service.GenerativeAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final GenerativeAIService generativeAIService;

    @PostMapping("/recommend")
    public ResponseEntity<ApiResponse<RecommendationResponse>> recommend() {
        String authenticatedEmail = getAuthenticatedEmail();
        RecommendationResponse response = aiService.recommend(authenticatedEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "Recommendations generated successfully", response));
    }

    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<AIInsightResponse>> getInsights() {
        String authenticatedEmail = getAuthenticatedEmail();
        AIInsightResponse insights = generativeAIService.getInsights(authenticatedEmail);
        return ResponseEntity.ok(new ApiResponse<>(true, "AI insights generated successfully", insights));
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}

