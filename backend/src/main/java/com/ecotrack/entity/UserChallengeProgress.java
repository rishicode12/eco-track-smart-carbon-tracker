package com.ecotrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_challenge_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_challenge_progress", columnNames = {"user_id", "challenge_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChallengeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Integer rewardPointsEarned;

    @Column(nullable = false)
    private String badgeEarned;

    @PrePersist
    public void prePersist() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }
}

