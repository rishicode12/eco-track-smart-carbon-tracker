package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.UserBadgeDto;
import com.ecotrack.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserBadgeDto>>> getUserBadges(@PathVariable Long userId) {
        List<UserBadgeDto> badges = badgeService.getUserBadges(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "User badges fetched successfully", badges));
    }
}
