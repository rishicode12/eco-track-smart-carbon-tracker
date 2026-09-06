package com.ecotrack.service.impl;

import com.ecotrack.dto.CarbonEmissionRequest;
import com.ecotrack.dto.CarbonEmissionResponse;
import com.ecotrack.entity.Goal;
import com.ecotrack.repository.GoalRepository;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.Challenge;
import com.ecotrack.entity.User;
import com.ecotrack.entity.UserChallengeProgress;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.ChallengeRepository;
import com.ecotrack.repository.GoalRepository;
import com.ecotrack.repository.UserChallengeProgressRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.CarbonEmissionService;
import com.ecotrack.service.EcoScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
@RequiredArgsConstructor
public class CarbonEmissionServiceImpl implements CarbonEmissionService {

    private static final Logger log = LogManager.getLogger(CarbonEmissionServiceImpl.class);

    private static final int LOW_CARBON_XP = 50;
    private static final int STANDARD_XP = 20;
    private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserRepository userRepository;
    private final EcoScoreService ecoScoreService;
    private final UserChallengeProgressRepository userChallengeProgressRepository;
    private final ChallengeRepository challengeRepository;
    private final GoalRepository goalRepository;

    @Override
    @Transactional
    public CarbonEmissionResponse createEmission(String email, CarbonEmissionRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CarbonEmission emission = new CarbonEmission();
        emission.setUser(user);
        emission.setDescription(request.getDescription());

        // 1. Smart Category Normalization (Crucial for Dashboard UI)
        String normalizedCategory = normalizeSmartCategory(request.getActivityCategory());
        emission.setActivityCategory(normalizedCategory);

        // 2. Exact Time Mapping
        if (request.getLoggedAt() != null) {
            emission.setCreatedAt(request.getLoggedAt());
        } else {
            emission.setCreatedAt(LocalDateTime.now());
        }

        // 3. Ensure variables are strictly ZERO before adding
        resetCategoryValues(emission);

        // 4. Calculate water emission if category is Water
        if ("Water".equals(normalizedCategory)) {
            BigDecimal waterConsumed = request.getWaterConsumed();
            String waterUnit = request.getWaterUnit();
            if (waterConsumed != null) {
                BigDecimal waterFactor = BigDecimal.valueOf(0.000344).setScale(4, RoundingMode.HALF_UP);
                BigDecimal consumed = waterConsumed;
                if ("Gallons".equalsIgnoreCase(waterUnit)) {
                    consumed = consumed.multiply(BigDecimal.valueOf(3.785)).setScale(4, RoundingMode.HALF_UP);
                }
                BigDecimal waterEmission = consumed.multiply(waterFactor).setScale(4, RoundingMode.HALF_UP);
                emission.setWaterEmission(waterEmission);
            }
        }

        // 5. Apply the impact to the specific bucket (So Dashboard icons work)
        applyCategoryEmission(emission, normalizedCategory, request.getCo2Impact());

        // 5. Calculate total
        emission.calculateTotalEmission();

        // Safety check: ensure tiny non-zero values don't trigger DB constraint violations
        if (emission.getTotalEmission().compareTo(BigDecimal.ZERO) > 0
                && emission.getTotalEmission().compareTo(new BigDecimal("0.0001")) < 0) {
            emission.setTotalEmission(new BigDecimal("0.0001"));
        }

        try {
            CarbonEmission saved = carbonEmissionRepository.save(emission);

            // 6. Gamification: award XP for the logged activity.
            int baseXp = isLowCarbonAction(saved, normalizedCategory) ? LOW_CARBON_XP : STANDARD_XP;
            ecoScoreService.awardXp(user.getId(), baseXp);

            // 7. Re-evaluate badge unlocks now that this emission is on record.
            ecoScoreService.evaluateAndUnlockBadges(user.getId());

            // 8. Auto-sync active challenges whose category matches this activity.
            syncChallengeProgress(user, normalizedCategory, saved.getTotalEmission());

            // 9. Auto-sync active goals whose intent matches this activity.
            syncGoalProgress(email, normalizedCategory, saved);

            return mapToResponse(saved);
        } catch (Exception e) {
            log.error("Error saving carbon emission: {}", e.getMessage());
            throw new ResourceNotFoundException("Unable to save carbon activity: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarbonEmissionResponse> getUserEmissions(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return carbonEmissionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public CarbonEmissionResponse updateEmission(Long id, String email, CarbonEmissionRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CarbonEmission emission = carbonEmissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carbon activity not found"));

        if (!emission.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Carbon activity not found");
        }

        String normalizedCategory = normalizeSmartCategory(request.getActivityCategory());
        emission.setActivityCategory(normalizedCategory);
        emission.setDescription(request.getDescription());

        if (request.getLoggedAt() != null) {
            emission.setCreatedAt(request.getLoggedAt());
        }

        // Reset specific categories before recalculating
        resetCategoryValues(emission);

        // Calculate water emission if category is Water
        if ("Water".equals(normalizedCategory)) {
            BigDecimal waterConsumed = request.getWaterConsumed();
            String waterUnit = request.getWaterUnit();
            if (waterConsumed != null) {
                BigDecimal waterFactor = BigDecimal.valueOf(0.000344).setScale(4, RoundingMode.HALF_UP);
                BigDecimal consumed = waterConsumed;
                if ("Gallons".equalsIgnoreCase(waterUnit)) {
                    consumed = consumed.multiply(BigDecimal.valueOf(3.785)).setScale(4, RoundingMode.HALF_UP);
                }
                BigDecimal waterEmission = consumed.multiply(waterFactor).setScale(4, RoundingMode.HALF_UP);
                emission.setWaterEmission(waterEmission);
            }
        }

        applyCategoryEmission(emission, normalizedCategory, request.getCo2Impact());
        emission.calculateTotalEmission();

        CarbonEmission updated = carbonEmissionRepository.save(emission);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteEmission(Long id, String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CarbonEmission emission = carbonEmissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carbon activity not found"));

        if (!emission.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Carbon activity not found");
        }

        carbonEmissionRepository.delete(emission);
    }

    private void resetCategoryValues(CarbonEmission emission) {
        emission.setTransportationEmission(BigDecimal.ZERO);
        emission.setElectricityEmission(BigDecimal.ZERO);
        emission.setFoodEmission(BigDecimal.ZERO);
        emission.setWasteEmission(BigDecimal.ZERO);
        emission.setWaterEmission(BigDecimal.ZERO);
    }

    private void applyCategoryEmission(CarbonEmission emission, String category, BigDecimal impact) {
        if (impact == null) {
            impact = BigDecimal.ZERO;
        }

        switch (category) {
            case "Transport":
                emission.setTransportationEmission(impact);
                break;
            case "Energy":
                emission.setElectricityEmission(impact);
                break;
            case "Food":
                emission.setFoodEmission(impact);
                break;
            case "Waste":
                emission.setWasteEmission(impact);
                break;
        }
    }

    // 🌟 ENHANCED: Smart String Matching!
    private String normalizeSmartCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Transport"; // Safe fallback
        }

        String text = category.trim().toLowerCase();

        // Agar sentence mein yeh words hain, toh automatically sahi category lag jayegi
        if (text.contains("transport") || text.contains("commute") || text.contains("car") || text.contains("vehicle") || text.contains("bus")) {
            return "Transport";
        } else if (text.contains("energy") || text.contains("electricity") || text.contains("gas") || text.contains("heat")) {
            return "Energy";
        } else if (text.contains("food") || text.contains("diet") || text.contains("meal") || text.contains("vegetarian") || text.contains("meat")) {
            return "Food";
        } else if (text.contains("waste") || text.contains("garbage") || text.contains("trash") || text.contains("recycle")) {
            return "Waste";
        } else if (text.contains("water") || text.contains("liters") || text.contains("gallons") || text.contains("consumed")) {
            return "Water";
        }

        return "Transport"; // Default fallback if nothing matches
    }

    /**
     * A "reduction / low-carbon action" is a zero-emission log (e.g. walking,
     * biking, energy saved) or one explicitly describing such an action.
     */
    private boolean isLowCarbonAction(CarbonEmission emission, String category) {
        BigDecimal categoryImpact = switch (category) {
            case "Transport" -> emission.getTransportationEmission();
            case "Energy" -> emission.getElectricityEmission();
            case "Food" -> emission.getFoodEmission();
            case "Waste" -> emission.getWasteEmission();
            default -> emission.getTotalEmission();
        };

        if (categoryImpact != null && categoryImpact.signum() == 0) {
            return true;
        }

        String description = emission.getDescription() == null
                ? ""
                : emission.getDescription().toLowerCase();
        return description.contains("walk")
                || description.contains("bike")
                || description.contains("cycl")
                || description.contains("recycl")
                || description.contains("compost")
                || description.contains("saved")
                || description.contains("saving")
                || description.contains("solar")
                || description.contains("public transport");
    }

    /**
     * Advances all IN_PROGRESS challenges for the user whose category matches the
     * logged activity. Challenges that reach their target are completed and their
     * bonus reward points are granted as XP.
     */
    private void syncChallengeProgress(User user, String activityCategory, BigDecimal co2Impact) {
        if (co2Impact == null || co2Impact.signum() == 0) {
            return;
        }

        double increment = co2Impact.doubleValue();
        List<UserChallengeProgress> inProgress =
                userChallengeProgressRepository.findByUserIdAndStatus(user.getId(), IN_PROGRESS_STATUS);

        for (UserChallengeProgress progress : inProgress) {
            Challenge challenge = progress.getChallenge();
            if (challenge == null || challenge.getCategory() == null) {
                continue;
            }

            if (!challenge.getCategory().equalsIgnoreCase(activityCategory)) {
                continue;
            }

            double target = challenge.getTargetGoal() != null ? challenge.getTargetGoal() : Double.MAX_VALUE;
            double current = progress.getCurrentProgress() != null ? progress.getCurrentProgress() : 0.0;
            double newProgress = Math.min(current + increment, target);

            progress.setCurrentProgress(newProgress);

            if (newProgress >= target && target != Double.MAX_VALUE) {
                progress.setStatus(COMPLETED_STATUS);
                progress.setCompletedAt(LocalDateTime.now());

                if (challenge.getRewardPoints() != null) {
                    ecoScoreService.awardXp(user.getId(), challenge.getRewardPoints());
                }
            }

            userChallengeProgressRepository.save(progress);
        }
    }

    /**
     * Advances all incomplete goals for the user whose intent matches the logged activity.
     * Goals that reach their target carbon reduction are marked as completed.
     * Uses case-insensitive email lookup and persists all changes to the database.
     */
    private void syncGoalProgress(String email, String activityCategory, CarbonEmission emission) {
        // Step 1: Fetch active goals using case-insensitive email lookup
        List<Goal> activeGoals = goalRepository.findByUserEmailIgnoreCaseAndIsCompletedFalse(email);
        
        // Step 2: Log how many active goals were found
        log.info("Found {} active goals for user email: {}", activeGoals.size(), email);
        
        if (activeGoals.isEmpty()) {
            log.debug("No active goals found for user: {}", email);
            return;
        }

        // Step 3: Iterate through active goals and check if they match
        for (Goal goal : activeGoals) {
            if (!matchesGoalIntent(goal.getTitle(), activityCategory)) {
                log.debug("Goal '{}' does not match activity category: {}", goal.getTitle(), activityCategory);
                continue;
            }

            // Step 4: Calculate the increment
            double increment;
            if (emission.getTotalEmission() != null && emission.getTotalEmission().compareTo(BigDecimal.ZERO) > 0) {
                increment = emission.getTotalEmission().doubleValue();
            } else {
                increment = 1.0; // Standard fallback increment
            }
            
            log.debug("Calculated increment for goal '{}': {}", goal.getTitle(), increment);

            // Step 5: Add increment to current progress, handling null safely
            Double currentProgress = goal.getCurrentProgress();
            double current = (currentProgress != null && currentProgress > 0) ? currentProgress : 0.0;
            
            Double targetReduction = goal.getTargetCarbonReduction();
            double target = (targetReduction != null && targetReduction > 0) ? targetReduction : 0.0;
            
            double updatedProgress = Math.min(current + increment, target);
            goal.setCurrentProgress(updatedProgress);
            
            log.debug("Goal '{}' progress updated: {} -> {}", goal.getTitle(), current, updatedProgress);

            // Step 6: Check if goal is completed
            if (target > 0 && updatedProgress >= target) {
                goal.setIsCompleted(true);
                log.info("Goal '{}' has been completed! Progress: {}/{}", goal.getTitle(), updatedProgress, target);
            }

            // Step 7: CRITICAL - Persist the change to the database
            goalRepository.save(goal);
            log.info("Goal '{}' has been updated and saved to database. Progress: {}/{}", 
                    goal.getTitle(), updatedProgress, target);
        }
    }

    private boolean matchesGoalIntent(String goalTitle, String category) {
    if (goalTitle == null || category == null) return false;
    String title = goalTitle.toLowerCase();

    return switch (category.toLowerCase()) {
        case "waste" -> title.contains("waste") || title.contains("garbage") 
                     || title.contains("plastic") || title.contains("recycl");
        case "transport" -> title.contains("transport") || title.contains("commute") 
                         || title.contains("car") || title.contains("cycle") || title.contains("train");
        case "energy" -> title.contains("energy") || title.contains("electric") || title.contains("power");
        case "water" -> title.contains("water") || title.contains("saving");
        case "food" -> title.contains("food") || title.contains("meat") || title.contains("diet");
        default -> false;
    };
}
    private double calculateGoalIncrement(CarbonEmission emission, String activityCategory, boolean isLowCarbon) {
        BigDecimal categoryImpact = switch (activityCategory) {
            case "Transport" -> emission.getTransportationEmission();
            case "Energy" -> emission.getElectricityEmission();
            case "Food" -> emission.getFoodEmission();
            case "Waste" -> emission.getWasteEmission();
            case "Water" -> emission.getWaterEmission();
            default -> emission.getTotalEmission();
        };

        double impact = categoryImpact != null ? categoryImpact.doubleValue() : 0.0;

        if (isLowCarbon && impact == 0.0) {
            return 1.0;
        }

        return Math.max(impact, 0.1);
    }

    private CarbonEmissionResponse mapToResponse(CarbonEmission emission) {
        return CarbonEmissionResponse.builder()
                .id(emission.getId())
                .activityCategory(emission.getActivityCategory())
                .co2Impact(emission.getTotalEmission())
                .description(emission.getDescription())
                .loggedAt(emission.getCreatedAt())
                .build();
    }
}