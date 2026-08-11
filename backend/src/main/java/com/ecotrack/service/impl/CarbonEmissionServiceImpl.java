package com.ecotrack.service.impl;

import com.ecotrack.dto.CarbonEmissionRequest;
import com.ecotrack.dto.CarbonEmissionResponse;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.CarbonEmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarbonEmissionServiceImpl implements CarbonEmissionService {

    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserRepository userRepository;

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

        // 4. Apply the impact to the specific bucket (So Dashboard icons work)
        applyCategoryEmission(emission, normalizedCategory, request.getCo2Impact());

        // 5. Calculate total
        emission.calculateTotalEmission();

        CarbonEmission saved = carbonEmissionRepository.save(emission);
        return mapToResponse(saved);
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
        }

        return "Transport"; // Default fallback if nothing matches
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