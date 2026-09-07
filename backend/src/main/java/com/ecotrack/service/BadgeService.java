package com.ecotrack.service;

import com.ecotrack.dto.UserBadgeDto;

import java.util.List;

public interface BadgeService {

    void awardBadge(Long userId, String badgeName, String badgeType, String description);

    void checkAndAwardBadges(Long userId);

    List<UserBadgeDto> getUserBadges(Long userId);
}
