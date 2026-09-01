package com.ecotrack.repository;

import com.ecotrack.entity.Notification;
import com.ecotrack.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    Notification findByIdAndUserId(Long notificationId, Long userId);
}