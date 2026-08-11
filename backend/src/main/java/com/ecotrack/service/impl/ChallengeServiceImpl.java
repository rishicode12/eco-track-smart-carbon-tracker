package com.ecotrack.service.impl;

import com.ecotrack.dto.ChallengeCompletionRequest;
import com.ecotrack.dto.ChallengeCompletionResponse;
import com.ecotrack.dto.ChallengeResponse;
import com.ecotrack.dto.LeaderboardResponse;
import com.ecotrack.entity.Challenge;
import com.ecotrack.entity.ChallengeType;
import com.ecotrack.entity.User;
import com.ecotrack.entity.UserChallengeProgress;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.ChallengeRepository;
import com.ecotrack.repository.UserChallengeProgressRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private static final int BRONZE_THRESHOLD = 0;
    private static final int SILVER_THRESHOLD = 250;
    private static final int GOLD_THRESHOLD = 500;
    private static final int PLATINUM_THRESHOLD = 1000;

    private final ChallengeRepository challengeRepository;
    private final UserChallengeProgressRepository userChallengeProgressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getDailyChallenges(String authenticatedEmail) {
        return challengeRepository.findByChallengeTypeAndActiveTrue(ChallengeType.DAILY).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getWeeklyChallenges(String authenticatedEmail) {
        return challengeRepository.findByChallengeTypeAndActiveTrue(ChallengeType.WEEKLY).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChallengeCompletionResponse completeChallenge(ChallengeCompletionRequest request, String authenticatedEmail) {
        User user = findUserByEmail(authenticatedEmail);
        Challenge challenge = challengeRepository.findByIdAndActiveTrue(request.getChallengeId())
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        if (userChallengeProgressRepository.existsByUserIdAndChallengeId(user.getId(), challenge.getId())) {
            throw new IllegalStateException("Challenge already completed");
        }

        int currentPoints = safePoints(user.getRewardPoints());
        int newTotalPoints = currentPoints + challenge.getRewardPoints();
        String badgeName = determineBadge(newTotalPoints);

        user.setRewardPoints(newTotalPoints);
        user.setBadgeName(badgeName);
        userRepository.save(user);

        UserChallengeProgress progress = UserChallengeProgress.builder()
                .user(user)
                .challenge(challenge)
                .rewardPointsEarned(challenge.getRewardPoints())
                .badgeEarned(badgeName)
                .build();
        userChallengeProgressRepository.save(progress);

        return ChallengeCompletionResponse.builder()
                .challengeId(challenge.getId())
                .challengeTitle(challenge.getTitle())
                .rewardPointsEarned(challenge.getRewardPoints())
                .totalRewardPoints(newTotalPoints)
                .badgeEarned(badgeName)
                .message("Challenge completed successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getLeaderboard(String authenticatedEmail) {
        AtomicInteger rank = new AtomicInteger(1);
        return userRepository.findLeaderboardUsers().stream()
                .limit(10)
                .map(user -> LeaderboardResponse.builder()
                        .rank(rank.getAndIncrement())
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .rewardPoints(safePoints(user.getRewardPoints()))
                        .badgeName(determineBadge(safePoints(user.getRewardPoints())))
                        .build())
                .toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ChallengeResponse mapToResponse(Challenge challenge) {
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .challengeType(challenge.getChallengeType())
                .rewardPoints(challenge.getRewardPoints())
                .badgeName(challenge.getBadgeName())
                .active(challenge.getActive())
                .createdAt(challenge.getCreatedAt())
                .build();
    }

    private int safePoints(Integer rewardPoints) {
        return rewardPoints == null ? 0 : rewardPoints;
    }

    private String determineBadge(int totalPoints) {
        if (totalPoints >= PLATINUM_THRESHOLD) {
            return "Platinum";
        }
        if (totalPoints >= GOLD_THRESHOLD) {
            return "Gold";
        }
        if (totalPoints >= SILVER_THRESHOLD) {
            return "Silver";
        }
        return "Bronze";
    }
}

