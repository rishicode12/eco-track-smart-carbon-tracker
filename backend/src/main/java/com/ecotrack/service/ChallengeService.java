package com.ecotrack.service;

import com.ecotrack.dto.ChallengeCompletionRequest;
import com.ecotrack.dto.ChallengeCompletionResponse;
import com.ecotrack.dto.ChallengeResponse;
import com.ecotrack.dto.LeaderboardResponse;

import java.util.List;

public interface ChallengeService {

    List<ChallengeResponse> getDailyChallenges(String authenticatedEmail);

    List<ChallengeResponse> getWeeklyChallenges(String authenticatedEmail);

    ChallengeCompletionResponse completeChallenge(ChallengeCompletionRequest request, String authenticatedEmail);

    List<LeaderboardResponse> getLeaderboard(String authenticatedEmail);
}

