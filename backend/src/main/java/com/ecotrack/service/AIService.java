package com.ecotrack.service;

import com.ecotrack.dto.RecommendationResponse;

public interface AIService {
    RecommendationResponse recommend(String authenticatedEmail);
}
