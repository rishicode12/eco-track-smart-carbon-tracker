package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.UserBadgeDto;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserBadgeDto>>> getMyBadges(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "No authenticated user", List.of()));
        }
        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<UserBadgeDto> badges = badgeService.getUserBadges(user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "User badges fetched successfully", badges));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserBadgeDto>>> getUserBadges(@PathVariable Long userId) {
        List<UserBadgeDto> badges = badgeService.getUserBadges(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "User badges fetched successfully", badges));
    }
}
