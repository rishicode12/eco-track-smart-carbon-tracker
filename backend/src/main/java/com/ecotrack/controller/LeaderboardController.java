package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.UserLeaderboardDto;
import com.ecotrack.dto.UserLeaderboardProjection;
import com.ecotrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserLeaderboardDto>>> getGlobalLeaderboard() {
        List<UserLeaderboardProjection> results = userRepository.findTop10GlobalLeaderboard();
        List<UserLeaderboardDto> leaderboard = results.stream()
                .map(p -> UserLeaderboardDto.builder()
                        .username(p.getUsername())
                        .totalXp(p.getTotalXp())
                        .level(p.getLevel())
                        .build())
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(true, "Global leaderboard fetched successfully", leaderboard));
    }
}
