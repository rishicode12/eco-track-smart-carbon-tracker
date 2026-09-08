package com.ecotrack.config;

import com.ecotrack.entity.Challenge;
import com.ecotrack.entity.ChallengeType;
import com.ecotrack.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeDataInitializer implements CommandLineRunner {

    private final ChallengeRepository challengeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedCanonicalChallenges();
    }

    private void seedCanonicalChallenges() {
        List<String> canonicalTitles = Arrays.asList(
                "Cycle to Work",
                "Energy Saving Challenge",
                "Plastic-Free Week",
                "Tree Plantation Drive"
        );

        // Delete any non-canonical dummy challenges
        List<Challenge> existing = challengeRepository.findAll();
        for (Challenge ch : existing) {
            if (!canonicalTitles.contains(ch.getTitle())) {
                log.info("Removing obsolete challenge: {}", ch.getTitle());
                challengeRepository.delete(ch);
            }
        }

        // 1. Cycle to Work
        cleanDuplicatesAndSave("Cycle to Work", Challenge.builder()
                .title("Cycle to Work")
                .description("Swap your car for a bike for at least 3 days a week. Track your miles.")
                .challengeType(ChallengeType.WEEKLY)
                .rewardPoints(350)
                .badgeName("ECO_WARRIOR")
                .category("TRANSPORT")
                .targetCategory("TRANSPORT")
                .targetGoal(3.0)
                .metric("commutes")
                .imageUrl("/uploads/cycle-to-work.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .active(true)
                .status("ACTIVE")
                .build());

        // 2. Energy Saving Challenge
        cleanDuplicatesAndSave("Energy Saving Challenge", Challenge.builder()
                .title("Energy Saving Challenge")
                .description("Reduce your home electricity consumption by 20% over 30 days.")
                .challengeType(ChallengeType.WEEKLY)
                .rewardPoints(500)
                .badgeName("ENERGY_SAVER")
                .category("ENERGY")
                .targetCategory("ENERGY")
                .targetGoal(20.0)
                .metric("kWh")
                .imageUrl("/uploads/Energy-Saving-Challenge.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .active(true)
                .status("ACTIVE")
                .build());

        // 3. Plastic-Free Week
        cleanDuplicatesAndSave("Plastic-Free Week", Challenge.builder()
                .title("Plastic-Free Week")
                .description("Avoid single-use plastics and log zero-waste activities for a week.")
                .challengeType(ChallengeType.WEEKLY)
                .rewardPoints(400)
                .badgeName("ZERO_WASTE")
                .category("WASTE")
                .targetCategory("WASTE")
                .targetGoal(7.0)
                .metric("days")
                .imageUrl("https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?q=80&w=600&auto=format&fit=crop")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(1))
                .active(true)
                .status("ACTIVE")
                .build());

        // 4. Tree Plantation Drive
        cleanDuplicatesAndSave("Tree Plantation Drive", Challenge.builder()
                .title("Tree Plantation Drive")
                .description("Collaborative goal: Plant 5,000 trees this month. Every tree counts.")
                .challengeType(ChallengeType.WEEKLY)
                .rewardPoints(1200)
                .badgeName("TREE_MASTER")
                .category("NATURE")
                .targetCategory("NATURE")
                .targetGoal(5.0)
                .metric("trees")
                .imageUrl("/uploads/tree-plantation-drive.jpg")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .active(true)
                .status("ACTIVE")
                .build());
    }

    private void cleanDuplicatesAndSave(String title, Challenge defaultChallenge) {
        List<Challenge> duplicates = challengeRepository.findAllByTitleIgnoreCase(title);
        if (duplicates.isEmpty()) {
            challengeRepository.save(defaultChallenge);
            log.info("Seeded challenge: {}", title);
        } else {
            Challenge primary = duplicates.get(0);
            primary.setImageUrl(defaultChallenge.getImageUrl());
            primary.setCategory(defaultChallenge.getCategory());
            primary.setTargetCategory(defaultChallenge.getTargetCategory());
            primary.setActive(true);
            challengeRepository.save(primary);

            // Delete extra duplicates if any
            for (int i = 1; i < duplicates.size(); i++) {
                log.info("Deleting duplicate challenge record for: {}", title);
                challengeRepository.delete(duplicates.get(i));
            }
        }
    }
}
