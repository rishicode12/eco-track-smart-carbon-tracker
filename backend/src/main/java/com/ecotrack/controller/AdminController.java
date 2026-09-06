package com.ecotrack.controller;

import com.ecotrack.dto.AdminUserResponse;
import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.ChallengeCreateRequest;
import com.ecotrack.dto.ChallengeResponse;
import com.ecotrack.entity.Role;
import com.ecotrack.entity.User;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.ChallengeService;
import com.ecotrack.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ChallengeService challengeService;
    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsers() {
        List<AdminUserResponse> users = userRepository.findAll().stream()
                .map(this::mapUser)
                .toList();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Users fetched successfully", users)
        );
    }

    @GetMapping("/challenges")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getAllChallenges() {
        List<ChallengeResponse> challenges = challengeService.getAllChallenges();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Challenges fetched successfully", challenges)
        );
    }

    @PostMapping("/challenges")
    public ResponseEntity<ApiResponse<ChallengeResponse>> createChallenge(
            @Valid @RequestBody ChallengeCreateRequest request
    ) {
        ChallengeResponse created = challengeService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Challenge created successfully", created));
    }

    @DeleteMapping("/challenges/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Challenge deleted successfully", null)
        );
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "User deactivated successfully", null)
        );
    }

    private AdminUserResponse mapUser(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(Role.fromString(user.getRole()).name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}