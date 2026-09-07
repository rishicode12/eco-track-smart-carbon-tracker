package com.ecotrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_challenges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_challenge", columnNames = {"user_id", "challenge_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Builder.Default
    @Column(name = "progress_count", nullable = false)
    private Double progressCount = 0.0;

    @Builder.Default
    @Column(length = 50, nullable = false)
    private String status = "ACTIVE";

    @Column(updatable = false)
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (progressCount == null) {
            progressCount = 0.0;
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
