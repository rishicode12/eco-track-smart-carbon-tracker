package com.ecotrack.controller;

import com.ecotrack.dto.DashboardResponse;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardData(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return ResponseEntity.ok(
                dashboardService.getDashboardData(
                        user.getId()
                )
        );
    }
}