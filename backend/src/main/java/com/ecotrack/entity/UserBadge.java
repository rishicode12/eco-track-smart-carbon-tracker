package com.ecotrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_name", nullable = false, length = 100)
    private String badgeName;

    @Column(name = "badge_type", nullable = false, length = 50)
    private String badgeType; // CHALLENGE, MILESTONE, STREAK

    @Column(length = 500)
    private String description;

    @Column(name = "earned_date", nullable = false, updatable = false)
    private LocalDateTime earnedDate;

    @PrePersist
    public void prePersist() {
        if (earnedDate == null) {
            earnedDate = LocalDateTime.now();
        }
    }
}
