package com.ecotrack.repository;

import com.ecotrack.entity.EcoProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EcoProfileRepository extends JpaRepository<EcoProfile, Long> {

    Optional<EcoProfile> findByUserId(Long userId);

    List<EcoProfile> findTop10ByOrderByTotalXpDescUserIdAsc();
}