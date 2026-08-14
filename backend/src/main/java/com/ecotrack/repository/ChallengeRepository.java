package com.ecotrack.repository;

import com.ecotrack.entity.Challenge;
import com.ecotrack.entity.ChallengeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByChallengeTypeAndActiveTrue(ChallengeType challengeType);

    Optional<Challenge> findByIdAndActiveTrue(Long id);

    List<Challenge> findAllByActiveTrue();
}

