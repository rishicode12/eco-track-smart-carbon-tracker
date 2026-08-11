package com.ecotrack.service;

import com.ecotrack.dto.AIInsightResponse;

public interface GenerativeAIService {
    AIInsightResponse getInsights(String userEmail);
}
