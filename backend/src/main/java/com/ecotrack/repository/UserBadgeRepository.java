package com.ecotrack.repository;

import com.ecotrack.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    boolean existsByUserIdAndBadgeName(Long userId, String badgeName);

    List<UserBadge> findByUserIdOrderByEarnedDateDesc(Long userId);
}
