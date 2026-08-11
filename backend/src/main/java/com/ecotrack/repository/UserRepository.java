package com.ecotrack.repository;

import com.ecotrack.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u ORDER BY COALESCE(u.rewardPoints, 0) DESC, u.id ASC")
    List<User> findLeaderboardUsers();

}
