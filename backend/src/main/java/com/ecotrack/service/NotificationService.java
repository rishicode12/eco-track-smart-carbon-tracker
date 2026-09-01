package com.ecotrack.service;

import com.ecotrack.dto.NotificationResponse;
import com.ecotrack.entity.Notification;
import com.ecotrack.entity.User;
import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getUserNotifications(String userEmail);

    long getUnreadCount(String userEmail);

    void markAsRead(Long notificationId, String userEmail);

    void markAllAsRead(String userEmail);

    void createNotification(User user, String title, String message, String type);
}