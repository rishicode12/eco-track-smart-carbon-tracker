package com.ecotrack.service;

import com.ecotrack.dto.EcoLeaderboardResponse;
import com.ecotrack.dto.EcoProfileResponse;
import com.ecotrack.entity.EcoProfile;

import java.util.List;

public interface EcoScoreService {

    EcoProfile awardXp(Long userId, int xpAmount);

    EcoProfile evaluateAndUnlockBadges(Long userId);

    EcoProfileResponse getProfile(String authenticatedEmail);

    List<EcoLeaderboardResponse> getLeaderboard();
}