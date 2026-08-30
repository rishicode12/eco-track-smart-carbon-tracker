package com.ecotrack.service.impl;

import com.ecotrack.dto.EcoLeaderboardResponse;
import com.ecotrack.dto.EcoProfileResponse;
import com.ecotrack.entity.EcoProfile;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.EcoProfileRepository;
import com.ecotrack.repository.UserChallengeProgressRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.EcoScoreService;
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
        }

        if (zeroEmissionEnergyCount >= ENERGY_SAVER_THRESHOLD) {
            unlockBadge(profile, ENERGY_SAVER_BADGE);
        }

        if (zeroEmissionTransportCount > ZERO_EMISSION_TRANSPORT_THRESHOLD) {
            unlockBadge(profile, ECO_WARRIOR_BADGE);
        }

        if (wasteActivityCount >= ZERO_WASTE_THRESHOLD) {
            unlockBadge(profile, ZERO_WASTE_BADGE);
        }

        if (completedChallengeCount >= TREE_MASTER_THRESHOLD) {
            unlockBadge(profile, TREE_MASTER_BADGE);
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

        return EcoProfileResponse.builder()
                .userId(user.getId())
                .totalXp(profile.getTotalXp())
                .currentLevel(profile.getCurrentLevel())
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
}