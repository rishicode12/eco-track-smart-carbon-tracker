package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.NotificationResponse;
import com.ecotrack.entity.User;
import com.ecotrack.service.NotificationService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List; // <-- ADDED THIS

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<NotificationResponse> notifications = notificationService.getUserNotifications(email);
        long unreadCount = notificationService.getUnreadCount(email);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notifications retrieved successfully", notifications)
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable("id") @NotNull Long notificationId, // <-- FIXED MAPPING HERE
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        notificationService.markAsRead(notificationId, email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as read", null));
    }
}