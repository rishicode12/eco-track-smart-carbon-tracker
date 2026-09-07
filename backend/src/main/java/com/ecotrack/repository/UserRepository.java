package com.ecotrack.repository;

import com.ecotrack.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email = :email AND (u.isActive = true OR u.isActive IS NULL)")
    Optional<User> findByEmailAndIsActive(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND (u.isActive = true OR u.isActive IS NULL)")
    Optional<User> findByEmailIgnoreCaseAndIsActive(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    // Restores the contract for AuthController, UserServiceImpl, etc.
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND (u.isActive = true OR u.isActive IS NULL)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND (u.isActive = true OR u.isActive IS NULL)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u ORDER BY COALESCE(u.rewardPoints, 0) DESC, u.id ASC")
    List<User> findLeaderboardUsers();

    @Query(value = "SELECT u.full_name AS username, COALESCE(p.total_xp, u.reward_points, 0) AS totalXp, COALESCE(p.current_level, 1) AS level " +
                   "FROM users u LEFT JOIN eco_profiles p ON u.id = p.user_id " +
                   "ORDER BY COALESCE(p.total_xp, u.reward_points, 0) DESC LIMIT 10", nativeQuery = true)
    List<com.ecotrack.dto.UserLeaderboardProjection> findTop10GlobalLeaderboard();
}