package com.ecotrack.repository;

import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarbonEmissionRepository
        extends JpaRepository<CarbonEmission, Long> {

    List<CarbonEmission> findByUserId(Long userId);

    List<CarbonEmission> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<CarbonEmission> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);

    List<CarbonEmission> findByCreatedAtBetween(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.totalEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
            """)
    BigDecimal calculateTotalEmissionByUserId(
            @Param("userId") Long userId
    );

    @Query("""
            SELECT COALESCE(SUM(c.totalEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
            """)
    BigDecimal calculateEmissionSince(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.totalEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
              AND c.createdAt < :endDate
            """)
    BigDecimal calculateEmissionBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
            SELECT u.id
            FROM User u
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<Long> findUserIdByEmail(
            @Param("email") String email
    );

    @Query("""
            SELECT COALESCE(SUM(c.transportationEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
            """)
    BigDecimal sumTransportationEmission(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.electricityEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
            """)
    BigDecimal sumElectricityEmission(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.foodEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
            """)
    BigDecimal sumFoodEmission(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.wasteEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
            """)
    BigDecimal sumWasteEmission(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.totalEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
              AND c.createdAt < :endDate
            """)
    BigDecimal sumEmissionBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
            SELECT FUNCTION('TO_CHAR', c.createdAt, 'YYYY-MM')
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND c.createdAt >= :startDate
            GROUP BY FUNCTION('TO_CHAR', c.createdAt, 'YYYY-MM')
            ORDER BY FUNCTION('TO_CHAR', c.createdAt, 'YYYY-MM') DESC
            """)
    List<String> findDistinctMonthKeys(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
            SELECT COALESCE(SUM(c.totalEmission), 0)
            FROM CarbonEmission c
            WHERE c.user.id = :userId
              AND FUNCTION('TO_CHAR', c.createdAt, 'YYYY-MM') = :monthKey
            """)
    BigDecimal sumEmissionByMonthKey(
            @Param("userId") Long userId,
            @Param("monthKey") String monthKey
    );
}