package com.ecotrack.service.impl;

import com.ecotrack.entity.EcoProfile;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.EcoProfileRepository;
import com.ecotrack.service.BadgeEvaluationService;
import com.ecotrack.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeEvaluationServiceImpl implements BadgeEvaluationService {

    private final BadgeService badgeService;
    private final EcoProfileRepository ecoProfileRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;

    @Override
    @Transactional
    public void evaluateBadgesAfterActivity(Long userId, String category) {
        if (userId == null) {
            return;
        }

        EcoProfile profile = ecoProfileRepository.findByUserId(userId).orElse(null);
        int totalXp = profile != null ? profile.getTotalXp() : 0;

        // 1. If category is 'ENERGY' and total energy XP >= 250 (or energy activity logged), award "ENERGY_SAVER"
        if ("ENERGY".equalsIgnoreCase(category)) {
            long energyCount = carbonEmissionRepository.countZeroEmissionEnergyActivities(userId);
            if (totalXp >= 250 || energyCount > 0) {
                badgeService.awardBadge(userId, "ENERGY_SAVER", "MILESTONE", "Save energy and reduce home electricity");
            }
        }

        // 2. If user.total_xp >= 1000, award "CARBON_CRUSHER"
        if (totalXp >= 1000) {
            badgeService.awardBadge(userId, "CARBON_CRUSHER", "MILESTONE", "Accumulate 1,000 total XP & cut carbon footprint");
        }

        // 3. If category is 'WASTE' and waste log count >= 10, award "ZERO_WASTE"
        if ("WASTE".equalsIgnoreCase(category)) {
            long wasteCount = carbonEmissionRepository.countWasteActivities(userId);
            if (wasteCount >= 10) {
                badgeService.awardBadge(userId, "ZERO_WASTE", "MILESTONE", "Log 10 waste reduction activities");
            }
        }

        // 4. Green Hero (First activity logged)
        long totalActivities = carbonEmissionRepository.findByUserId(userId).size();
        if (totalActivities >= 1) {
            badgeService.awardBadge(userId, "GREEN_HERO", "MILESTONE", "Complete your first eco activity");
        }

        // 5. Eco Warrior (5+ zero-emission transport activities)
        long transportZeroCount = carbonEmissionRepository.countZeroEmissionTransportActivities(userId);
        if (transportZeroCount >= 5) {
            badgeService.awardBadge(userId, "ECO_WARRIOR", "MILESTONE", "Log 5+ zero-emission transport activities");
        }

        // 6. Check generic XP milestones
        badgeService.checkAndAwardBadges(userId);
    }
}
