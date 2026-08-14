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

    @Column
    private LocalDateTime completedAt;

    @Column
    private Integer rewardPointsEarned;

    @Column
    private String badgeEarned;

    @Builder.Default
    private Double currentProgress = 0.0;

    @Builder.Default
    @Column(length = 50)
    private String status = "IN_PROGRESS";

    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (currentProgress == null) {
            currentProgress = 0.0;
        }
        if (status == null) {
            status = "IN_PROGRESS";
        }
    }
}

