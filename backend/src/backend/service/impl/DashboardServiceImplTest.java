package com.ecotrack.service.impl;

import com.ecotrack.dto.DashboardResponse;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.User;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private CarbonEmissionRepository carbonEmissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void shouldBuildDashboardForUser() {

        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .rewardPoints(100)
                .badgeName("Bronze")
                .build();

        CarbonEmission emission =
                CarbonEmission.builder()
                        .id(1L)
                        .user(user)
                        .activityCategory("Transport")
                        .transportationEmission(
                                new BigDecimal("8.00")
                        )
                        .electricityEmission(
                                BigDecimal.ZERO
                        )
                        .foodEmission(
                                BigDecimal.ZERO
                        )
                        .wasteEmission(
                                BigDecimal.ZERO
                        )
                        .totalEmission(
                                new BigDecimal("8.00")
                        )
                        .description(
                                "Commute by car-petrol (15 mi)"
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                carbonEmissionRepository
                        .calculateEmissionBetween(
                                eq(1L),
                                any(LocalDateTime.class),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(
                new BigDecimal("8.00")
        );

        when(
                carbonEmissionRepository
                        .calculateEmissionSince(
                                eq(1L),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(
                new BigDecimal("8.00")
        );

        when(
                carbonEmissionRepository
                        .findByUserIdOrderByCreatedAtDesc(1L)
        ).thenReturn(
                List.of(emission)
        );

        DashboardResponse response =
                dashboardService.getDashboardData(1L);

        assertNotNull(response);

        assertEquals(
                "Test User",
                response.getFullName()
        );

        assertEquals(
                Integer.valueOf(100),
                response.getRewardPoints()
        );

        assertEquals(
                "Bronze",
                response.getBadgeName()
        );

        assertEquals(
                new BigDecimal("8.00"),
                response.getTodayCarbon()
        );

        assertEquals(
                new BigDecimal("8.00"),
                response.getMonthlyCarbon()
        );

        assertNotNull(
                response.getRecentActivities()
        );

        assertEquals(
                1,
                response.getRecentActivities().size()
        );

        assertEquals(
                "Transport",
                response.getRecentActivities()
                        .get(0)
                        .getType()
        );

        assertEquals(
                new BigDecimal("8.00"),
                response.getRecentActivities()
                        .get(0)
                        .getImpact()
        );

        assertNotNull(
                response.getChartData()
        );

        assertEquals(
                7,
                response.getChartData()
                        .getLabels()
                        .size()
        );

        assertEquals(
                7,
                response.getChartData()
                        .getValues()
                        .size()
        );
    }
}