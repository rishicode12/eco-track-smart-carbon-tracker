package com.ecotrack.service;

import com.ecotrack.dto.AIInsightResponse;

public interface AIRuleService {
    AIInsightResponse generateInsights(String userEmail);
}
