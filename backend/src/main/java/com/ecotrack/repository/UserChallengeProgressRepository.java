package com.ecotrack.repository;

import com.ecotrack.entity.UserChallengeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserChallengeProgressRepository extends JpaRepository<UserChallengeProgress, Long> {

    boolean existsByUserIdAndChallengeId(Long userId, Long challengeId);

    List<UserChallengeProgress> findByUserIdOrderByCompletedAtDesc(Long userId);
}

