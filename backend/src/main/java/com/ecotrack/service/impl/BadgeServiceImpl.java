package com.ecotrack.service.impl;

import com.ecotrack.dto.UserBadgeDto;
import com.ecotrack.entity.EcoProfile;
import com.ecotrack.entity.User;
import com.ecotrack.entity.UserBadge;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.EcoProfileRepository;
import com.ecotrack.repository.UserBadgeRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.BadgeService;
import com.ecotrack.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final EcoProfileRepository ecoProfileRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void awardBadge(Long userId, String badgeName, String badgeType, String description) {
        if (badgeName == null || badgeName.trim().isEmpty()) {
            return;
        }

        // Idempotency check: Ensure the user doesn't already own this badge
        if (userBadgeRepository.existsByUserIdAndBadgeName(userId, badgeName.trim())) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserBadge badge = UserBadge.builder()
                .user(user)
                .badgeName(badgeName.trim())
                .badgeType(badgeType != null ? badgeType : "MILESTONE")
                .description(description != null ? description : "Badge earned!")
                .build();

        userBadgeRepository.save(badge);

        // Also add badge name to EcoProfile unlockedBadges list for backwards compatibility
        EcoProfile profile = ecoProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null && !profile.getUnlockedBadges().contains(badgeName.trim())) {
            profile.getUnlockedBadges().add(badgeName.trim());
            ecoProfileRepository.save(profile);
        }

        // Send unlock notification
        notificationService.createNotification(user, "Badge Unlocked!", "You earned the " + badgeName + " badge!", "BADGE_UNLOCK");
    }

    @Override
    @Transactional
    public void checkAndAwardBadges(Long userId) {
        EcoProfile profile = ecoProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return;

        int totalXp = profile.getTotalXp();

        // Milestone Badges
        if (totalXp >= 1000) {
            awardBadge(userId, "GREEN_HERO", "MILESTONE", "Earn 1,000 total XP");
        }
        if (totalXp >= 5000) {
            awardBadge(userId, "ECO_WARRIOR", "MILESTONE", "Cut your carbon footprint or reach 5,000 XP");
        }
        if (totalXp >= 10000) {
            awardBadge(userId, "PLANET_HERO", "MILESTONE", "Reach 10,000 XP max tier");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadgeDto> getUserBadges(Long userId) {
        return userBadgeRepository.findByUserIdOrderByEarnedDateDesc(userId).stream()
                .map(badge -> UserBadgeDto.builder()
                        .id(badge.getId())
                        .userId(badge.getUser().getId())
                        .badgeName(badge.getBadgeName())
                        .badgeType(badge.getBadgeType())
                        .description(badge.getDescription())
                        .earnedDate(badge.getEarnedDate())
                        .build())
                .toList();
    }
}
