package com.ecotrack.service.impl;

import com.ecotrack.dto.EcoLeaderboardResponse;
import com.ecotrack.dto.EcoProfileResponse;
import com.ecotrack.entity.EcoProfile;
import com.ecotrack.entity.Notification;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.EcoProfileRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.NotificationService;
import com.ecotrack.service.EcoScoreService;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.UserChallengeProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class EcoScoreServiceImpl implements EcoScoreService {

    private static final String GREEN_HERO_BADGE = "GREEN_HERO";
    private static final String ENERGY_SAVER_BADGE = "ENERGY_SAVER";
    private static final String ECO_WARRIOR_BADGE = "ECO_WARRIOR";
    private static final String ZERO_WASTE_BADGE = "ZERO_WASTE";
    private static final String TREE_MASTER_BADGE = "TREE_MASTER";

    private static final long GREEN_HERO_XP_THRESHOLD = 1000;
    private static final long ENERGY_SAVER_THRESHOLD = 3;
    private static final long ZERO_EMISSION_TRANSPORT_THRESHOLD = 5;
    private static final long ZERO_WASTE_THRESHOLD = 5;
    private static final long TREE_MASTER_THRESHOLD = 2;
    private static final String COMPLETED_STATUS = "COMPLETED";

    private static final int LEVEL_2_XP = 1000;
    private static final int LEVEL_3_XP = 2500;
    private static final int LEVEL_4_XP = 5000;
    private static final int LEVEL_5_XP = 10000;

    private final EcoProfileRepository ecoProfileRepository;
    private final UserRepository userRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserChallengeProgressRepository userChallengeProgressRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public EcoProfile awardXp(Long userId, int xpAmount) {
        if (xpAmount <= 0) {
            throw new IllegalArgumentException("xpAmount must be greater than zero");
        }

        User user = findUserById(userId);
        EcoProfile profile = getOrCreateProfile(user);

        int newTotalXp = profile.getTotalXp() + xpAmount;
        profile.setTotalXp(newTotalXp);

        int newLevel = computeLevel(newTotalXp);
        if (newLevel > profile.getCurrentLevel()) {
            // Level up - send notification
            String levelUpTitle = "Level Up!";
            String levelUpMessage = "Congratulations! You reached level " + newLevel;
            notificationService.createNotification(user, levelUpTitle, levelUpMessage, "LEVEL_UP");
            profile.setCurrentLevel(newLevel);
        }

        return ecoProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public EcoProfile evaluateAndUnlockBadges(Long userId) {
        User user = findUserById(userId);
        EcoProfile profile = getOrCreateProfile(user);

        long zeroEmissionTransportCount = carbonEmissionRepository
                .countZeroEmissionTransportActivities(userId);
        long zeroEmissionEnergyCount = carbonEmissionRepository
                .countZeroEmissionEnergyActivities(userId);
        long wasteActivityCount = carbonEmissionRepository
                .countWasteActivities(userId);
        long completedChallengeCount = userChallengeProgressRepository
                .countByUserIdAndStatus(userId, COMPLETED_STATUS);

        if (profile.getTotalXp() >= GREEN_HERO_XP_THRESHOLD) {
            unlockBadge(profile, GREEN_HERO_BADGE);
            // Badge unlock - send notification
            notificationService.createNotification(user, "Badge Unlocked!", "You earned the GREEN_HERO badge", "BADGE_UNLOCK");
        }

        if (zeroEmissionEnergyCount >= ENERGY_SAVER_THRESHOLD) {
            unlockBadge(profile, ENERGY_SAVER_BADGE);
            notificationService.createNotification(user, "Badge Unlocked!", "You earned the ENERGY_SAVER badge", "BADGE_UNLOCK");
        }

        if (zeroEmissionTransportCount > ZERO_EMISSION_TRANSPORT_THRESHOLD) {
            unlockBadge(profile, ECO_WARRIOR_BADGE);
            notificationService.createNotification(user, "Badge Unlocked!", "You earned the ECO_WARRIOR badge", "BADGE_UNLOCK");
        }

        if (wasteActivityCount >= ZERO_WASTE_THRESHOLD) {
            unlockBadge(profile, ZERO_WASTE_BADGE);
            notificationService.createNotification(user, "Badge Unlocked!", "You earned the ZERO_WASTE badge", "BADGE_UNLOCK");
        }

        if (completedChallengeCount >= TREE_MASTER_THRESHOLD) {
            unlockBadge(profile, TREE_MASTER_BADGE);
            notificationService.createNotification(user, "Badge Unlocked!", "You earned the TREE_MASTER badge", "BADGE_UNLOCK");
        }

        return ecoProfileRepository.save(profile);
    }

    private void unlockBadge(EcoProfile profile, String badge) {
        if (!profile.getUnlockedBadges().contains(badge)) {
            profile.getUnlockedBadges().add(badge);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EcoProfileResponse getProfile(String authenticatedEmail) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EcoProfile profile = getOrCreateProfile(user);

        int totalXp = profile.getTotalXp();
        int currentLevel = profile.getCurrentLevel();
        int nextLevelThreshold = getThresholdForLevel(currentLevel + 1);
        int currentLevelBaseXp = getThresholdForLevel(currentLevel);

        int xpToNextLevel = Math.max(0, nextLevelThreshold - totalXp);
        double progressPercentage = 0.0;
        if (nextLevelThreshold > currentLevelBaseXp) {
            int levelSpanXp = nextLevelThreshold - currentLevelBaseXp;
            progressPercentage = Math.min(100.0,
                    ((double) (totalXp - currentLevelBaseXp) / levelSpanXp) * 100);
        }

        return EcoProfileResponse.builder()
                .userId(user.getId())
                .totalXp(totalXp)
                .currentLevel(currentLevel)
                .levelName(getLevelName(currentLevel))
                .xpToNextLevel(xpToNextLevel)
                .progressPercentage(Math.round(progressPercentage * 10.0) / 10.0)
                .unlockedBadges(profile.getUnlockedBadges())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcoLeaderboardResponse> getLeaderboard() {
        AtomicInteger rank = new AtomicInteger(1);
        return ecoProfileRepository.findTop10ByOrderByTotalXpDescUserIdAsc().stream()
                .map(profile -> EcoLeaderboardResponse.builder()
                        .rank(rank.getAndIncrement())
                        .userId(profile.getUser().getId())
                        .fullName(profile.getUser().getFullName())
                        .totalXp(profile.getTotalXp())
                        .currentLevel(profile.getCurrentLevel())
                        .build())
                .toList();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private EcoProfile getOrCreateProfile(User user) {
        return ecoProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> ecoProfileRepository.save(
                        EcoProfile.builder().user(user).build()));
    }

    private int computeLevel(int totalXp) {
        if (totalXp >= LEVEL_5_XP) {
            return 5;
        }
        if (totalXp >= LEVEL_4_XP) {
            return 4;
        }
        if (totalXp >= LEVEL_3_XP) {
            return 3;
        }
        if (totalXp >= LEVEL_2_XP) {
            return 2;
        }
        return 1;
    }

    private int getThresholdForLevel(int level) {
        return switch (level) {
            case 2 -> LEVEL_2_XP;
            case 3 -> LEVEL_3_XP;
            case 4 -> LEVEL_4_XP;
            case 5 -> LEVEL_5_XP;
            default -> 0;
        };
    }

    private String getLevelName(int level) {
        return switch (level) {
            case 1 -> "Explorer";
            case 2 -> "Advocate";
            case 3 -> "Champion";
            case 4 -> "Guardian";
            case 5 -> "Planet Hero";
            default -> "Explorer";
        };
    }
}