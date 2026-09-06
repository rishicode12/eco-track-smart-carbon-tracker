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

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByEmailIgnoreCaseAndIsActiveTrue(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Restores the contract for AuthController, UserServiceImpl, etc.
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.isActive = true")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    // Restores the contract for AIServiceImpl, ChallengeServiceImpl, etc.
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u ORDER BY COALESCE(u.rewardPoints, 0) DESC, u.id ASC")
    List<User> findLeaderboardUsers();

}