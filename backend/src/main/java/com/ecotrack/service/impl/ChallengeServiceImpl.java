package com.ecotrack.service.impl;

import com.ecotrack.dto.ChallengeCompletionRequest;
import com.ecotrack.dto.ChallengeCompletionResponse;
import com.ecotrack.dto.ChallengeCreateRequest;
import com.ecotrack.dto.ChallengeResponse;
import com.ecotrack.dto.LeaderboardResponse;
import com.ecotrack.dto.NotificationResponse;
import com.ecotrack.dto.UpdateProgressRequest;
import com.ecotrack.entity.Challenge;
import com.ecotrack.entity.ChallengeType;
import com.ecotrack.entity.Notification;
import com.ecotrack.entity.User;
import com.ecotrack.entity.UserChallenge;
import com.ecotrack.entity.UserChallengeProgress;
import com.ecotrack.exception.MaxChallengesExceededException;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.ChallengeRepository;
import com.ecotrack.repository.UserChallengeRepository;
import com.ecotrack.repository.UserChallengeProgressRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.service.BadgeService;
import com.ecotrack.service.ChallengeService;
import com.ecotrack.service.EcoScoreService;
import com.ecotrack.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private static final int BRONZE_THRESHOLD = 0;
    private static final int SILVER_THRESHOLD = 250;
    private static final int GOLD_THRESHOLD = 500;
    private static final int PLATINUM_THRESHOLD = 1000;

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserChallengeProgressRepository userChallengeProgressRepository;
    private final UserRepository userRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final NotificationService notificationService;
    private final EcoScoreService ecoScoreService;
    private final BadgeService badgeService;

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

        // Task 1 & 3: Route XP reward through centralized ecoScoreService.addXp
        int rewardPoints = challenge.getRewardPoints() != null ? challenge.getRewardPoints() : 0;
        if (rewardPoints > 0) {
            ecoScoreService.addXp(user.getId(), rewardPoints);
        }

        // Task 3: Award challenge badge via BadgeService
        if (challenge.getBadgeName() != null && !challenge.getBadgeName().isEmpty()) {
            badgeService.awardBadge(user.getId(), challenge.getBadgeName(), "CHALLENGE", challenge.getDescription());
        }

        String badgeName = challenge.getBadgeName() != null ? challenge.getBadgeName() : "Bronze";

        UserChallengeProgress progress = UserChallengeProgress.builder()
                .user(user)
                .challenge(challenge)
                .rewardPointsEarned(rewardPoints)
                .badgeEarned(badgeName)
                .status("COMPLETED")
                .completedAt(LocalDateTime.now())
                .build();
        userChallengeProgressRepository.save(progress);

        // Send notification for challenge completion
        notificationService.createNotification(user, "Challenge Completed!", "You completed the challenge: " + challenge.getTitle(), "CHALLENGE_COMPLETED");

        return ChallengeCompletionResponse.builder()
                .challengeId(challenge.getId())
                .challengeTitle(challenge.getTitle())
                .rewardPointsEarned(rewardPoints)
                .totalRewardPoints(user.getRewardPoints())
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

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getAllActiveChallenges(String email) {
        User user = findUserByEmail(email);
        return challengeRepository.findAllByActiveTrue().stream()
                .map(challenge -> mapToResponse(challenge,
                        userChallengeProgressRepository.findByUserIdAndChallengeId(user.getId(), challenge.getId()),
                        user.getId()))
                .toList();
    }

    @Override
    @Transactional
    public ChallengeResponse joinChallenge(Long challengeId, String email) {
        User user = findUserByEmail(email);
        return joinChallenge(user.getId(), challengeId);
    }

    @Override
    @Transactional
    public ChallengeResponse joinChallenge(Long userId, Long challengeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        if (!Boolean.TRUE.equals(challenge.getActive())) {
            throw new IllegalStateException("Challenge is not active");
        }

        // CRITICAL LIMIT: A user can only have a maximum of 2 ACTIVE challenges at the same time.
        long activeCountInUserChallenges = userChallengeRepository.countByUserIdAndStatus(user.getId(), "ACTIVE");
        long activeCountInUserProgress = userChallengeProgressRepository.countByUserIdAndStatus(user.getId(), "IN_PROGRESS");
        long totalActive = Math.max(activeCountInUserChallenges, activeCountInUserProgress);

        if (totalActive >= 2) {
            throw new MaxChallengesExceededException("A user can only have a maximum of 2 ACTIVE challenges at the same time.");
        }

        if (userChallengeProgressRepository.findByUserIdAndChallengeId(user.getId(), challenge.getId()).isPresent()
                || userChallengeRepository.findByUserIdAndChallengeId(user.getId(), challenge.getId()).isPresent()) {
            throw new IllegalStateException("Challenge already joined");
        }

        UserChallenge userChallenge = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .progressCount(0.0)
                .status("ACTIVE")
                .build();
        userChallengeRepository.save(userChallenge);

        UserChallengeProgress progress = UserChallengeProgress.builder()
                .user(user)
                .challenge(challenge)
                .currentProgress(0.0)
                .status("IN_PROGRESS")
                .build();
        userChallengeProgressRepository.save(progress);

        return mapToResponse(challenge, Optional.of(progress), user.getId());
    }

    @Override
    @Transactional
    public ChallengeResponse updateProgress(Long challengeId, String email, UpdateProgressRequest request) {
        if (request == null || request.getProgressAdded() == null || request.getProgressAdded() <= 0) {
            throw new IllegalArgumentException("progressAdded must be greater than zero");
        }

        User user = findUserByEmail(email);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        UserChallengeProgress progress = userChallengeProgressRepository
                .findByUserIdAndChallengeId(user.getId(), challenge.getId())
                .orElseThrow(() -> new IllegalStateException("Challenge not joined"));

        if ("COMPLETED".equals(progress.getStatus())) {
            throw new IllegalStateException("Challenge already completed");
        }

        double target = challenge.getTargetGoal() != null ? challenge.getTargetGoal() : Double.MAX_VALUE;
        double current = progress.getCurrentProgress() != null ? progress.getCurrentProgress() : 0.0;
        double newProgress = Math.min(current + request.getProgressAdded(), target);

        progress.setCurrentProgress(newProgress);
        progress.setStatus("IN_PROGRESS");

        if (newProgress >= target) {
            progress.setStatus("COMPLETED");
            progress.setCompletedAt(LocalDateTime.now());

            int currentPoints = safePoints(user.getRewardPoints());
            int newTotalPoints = currentPoints + challenge.getRewardPoints();
            String badgeName = determineBadge(newTotalPoints);

            user.setRewardPoints(newTotalPoints);
            user.setBadgeName(badgeName);
            userRepository.save(user);

            progress.setRewardPointsEarned(challenge.getRewardPoints());
            progress.setBadgeEarned(badgeName);

            // Send notification for challenge completion via progress
            notificationService.createNotification(user, "Challenge Completed!", "You completed the challenge: " + challenge.getTitle(), "CHALLENGE_COMPLETED");
        }

        userChallengeProgressRepository.save(progress);
        return mapToResponse(challenge, Optional.of(progress), user.getId());
    }

    @Override
    @Transactional
    public ChallengeResponse createChallenge(ChallengeCreateRequest request) {
        Challenge challenge = Challenge.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .challengeType(request.getChallengeType())
                .rewardPoints(request.getRewardPoints())
                .badgeName(request.getBadgeName())
                .category(request.getCategory())
                .targetGoal(request.getTargetGoal())
                .metric(request.getMetric())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        return mapToResponse(challengeRepository.save(challenge));
    }

    @Override
    @Transactional
    public void deleteChallenge(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        // Remove related user progress first to avoid FK constraint violations.
        userChallengeProgressRepository.deleteByChallengeId(challengeId);
        challengeRepository.delete(challenge);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getAllChallenges() {
        return challengeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateProgress(Long userId, Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        if (challenge.getTargetGoal() == null || challenge.getTargetGoal() <= 0) {
            return 0;
        }

        LocalDateTime start = challenge.getStartDate() != null
                ? challenge.getStartDate().atStartOfDay()
                : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = challenge.getEndDate() != null
                ? challenge.getEndDate().plusDays(1).atStartOfDay()
                : LocalDateTime.now();

        String category = challenge.getCategory() != null ? challenge.getCategory().toLowerCase() : "";
        double matchingCount = 0;

        switch (category) {
            case "waste":
                matchingCount = carbonEmissionRepository.countByActivityCategoryAndDateRange(
                        userId, "waste", start, end);
                break;
            case "transport":
                matchingCount = carbonEmissionRepository.countByActivityCategoryAndDateRange(
                        userId, "transport", start, end);
                break;
            case "energy":
                matchingCount = carbonEmissionRepository.countByActivityCategoryAndDateRange(
                        userId, "energy", start, end);
                break;
            case "nature":
                matchingCount = carbonEmissionRepository.countByActivityCategoryAndDateRange(
                        userId, "nature", start, end);
                break;
            default:
                matchingCount = carbonEmissionRepository.countByUserIdAndDateRange(
                        userId, start, end);
                break;
        }

        double percent = (matchingCount / challenge.getTargetGoal()) * 100;
        return (int) Math.min(100, Math.max(0, Math.round(percent)));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ChallengeResponse mapToResponse(Challenge challenge) {
        return mapToResponse(challenge, Optional.empty(), null);
    }

    private ChallengeResponse mapToResponse(Challenge challenge, Optional<UserChallengeProgress> progress, Long userId) {
        UserChallengeProgress userProgress = progress.orElse(null);
        boolean joined = userProgress != null;

        int calculatedProgress = 0;
        if (joined && userId != null) {
            calculatedProgress = calculateProgress(userId, challenge.getId());
        }

        return ChallengeResponse.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .challengeType(challenge.getChallengeType())
                .rewardPoints(challenge.getRewardPoints())
                .badgeName(challenge.getBadgeName())
                .category(challenge.getCategory())
                .active(challenge.getActive())
                .createdAt(challenge.getCreatedAt())
                .targetGoal(challenge.getTargetGoal())
                .metric(challenge.getMetric())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .status(challenge.getStatus())
                .isJoined(joined)
                .currentProgress((double) calculatedProgress)
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

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponse> searchChallenges(String query, String authenticatedEmail) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<Challenge> results = challengeRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                query.trim(), query.trim());

        return results.stream()
                .map(challenge -> ChallengeResponse.builder()
                        .id(challenge.getId())
                        .title(challenge.getTitle())
                        .description(challenge.getDescription())
                        .challengeType(challenge.getChallengeType())
                        .rewardPoints(challenge.getRewardPoints())
                        .badgeName(challenge.getBadgeName())
                        .category(challenge.getCategory())
                        .active(challenge.getActive())
                        .createdAt(challenge.getCreatedAt())
                        .targetGoal(challenge.getTargetGoal())
                        .metric(challenge.getMetric())
                        .startDate(challenge.getStartDate())
                        .endDate(challenge.getEndDate())
                        .status(challenge.getStatus())
                        .isJoined(false)
                        .currentProgress(0.0)
                        .build())
                .toList();
    }
}