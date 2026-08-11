package com.ecotrack.backend.service.impl;

import com.ecotrack.dto.ChallengeCompletionRequest;
import com.ecotrack.dto.ChallengeCompletionResponse;
import com.ecotrack.dto.LeaderboardResponse;
import com.ecotrack.entity.Challenge;
import com.ecotrack.entity.ChallengeType;
import com.ecotrack.entity.User;
import com.ecotrack.entity.UserChallengeProgress;
import com.ecotrack.repository.ChallengeRepository;
import com.ecotrack.repository.UserChallengeProgressRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.impl.ChallengeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UserChallengeProgressRepository userChallengeProgressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Test
    void shouldCompleteChallengeAndUpdateUserStats() {
        User user = User.builder()
                .id(1L)
                .fullName("Eco User")
                .email("user@example.com")
                .password("secret")
                .rewardPoints(100)
                .badgeName("Bronze")
                .build();

        Challenge challenge = Challenge.builder()
                .id(10L)
                .title("Use public transport")
                .description("Avoid using a private vehicle for one day")
                .challengeType(ChallengeType.DAILY)
                .rewardPoints(150)
                .badgeName("Green Starter")
                .active(true)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(challengeRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(challenge));
        when(userChallengeProgressRepository.existsByUserIdAndChallengeId(1L, 10L)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        ChallengeCompletionResponse response = challengeService.completeChallenge(
                new ChallengeCompletionRequest(10L),
                "user@example.com"
        );

        assertEquals(250, response.getTotalRewardPoints());
        assertEquals("Silver", response.getBadgeEarned());
        assertEquals(250, user.getRewardPoints());
        assertEquals("Silver", user.getBadgeName());

        ArgumentCaptor<UserChallengeProgress> progressCaptor = ArgumentCaptor.forClass(UserChallengeProgress.class);
        verify(userChallengeProgressRepository).save(progressCaptor.capture());
        assertEquals(150, progressCaptor.getValue().getRewardPointsEarned());
        assertEquals("Silver", progressCaptor.getValue().getBadgeEarned());
    }

    @Test
    void shouldReturnLeaderboardOrderedByRewardPoints() {
        User first = User.builder()
                .id(1L)
                .fullName("Top User")
                .email("top@example.com")
                .password("secret")
                .rewardPoints(900)
                .badgeName("Gold")
                .build();

        User second = User.builder()
                .id(2L)
                .fullName("Second User")
                .email("second@example.com")
                .password("secret")
                .rewardPoints(300)
                .badgeName("Silver")
                .build();

        when(userRepository.findLeaderboardUsers()).thenReturn(List.of(first, second));

        List<LeaderboardResponse> leaderboard = challengeService.getLeaderboard("user@example.com");

        assertEquals(2, leaderboard.size());
        assertEquals(1, leaderboard.get(0).getRank());
        assertEquals("Top User", leaderboard.get(0).getFullName());
        assertEquals(2, leaderboard.get(1).getRank());
        assertEquals("Second User", leaderboard.get(1).getFullName());
    }
}
