package com.ecotrack.utils;

import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    public String buildPrompt(User user, List<CarbonEmission> emissions) {
        String emissionSummary = buildEmissionSummary(emissions);
        String categorySummary = buildCategorySummary(emissions);

        return "Generate 5 personalized sustainability recommendations for the following user. "
                + "Return only valid JSON in this format: {\"recommendations\":[\"item1\",\"item2\",\"item3\",\"item4\",\"item5\"]}. "
                + "Do not add markdown, numbering, or extra text.\n\n"
                + "User: " + user.getFullName() + "\n"
                + "Email: " + user.getEmail() + "\n"
                + "Emission history summary: " + emissionSummary + "\n"
                + "Category totals: " + categorySummary + "\n"
                + "Focus on practical actions based on the user's current carbon footprint pattern.";
    }

    private String buildEmissionSummary(List<CarbonEmission> emissions) {
        if (emissions == null || emissions.isEmpty()) {
            return "No carbon emission history available.";
        }

        BigDecimal total = emissions.stream()
                .map(CarbonEmission::getTotalEmission)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "Records: " + emissions.size() + ", Total emissions: " + total;
    }

    private String buildCategorySummary(List<CarbonEmission> emissions) {
        if (emissions == null || emissions.isEmpty()) {
            return "No category data available.";
        }

        Map<String, BigDecimal> totals = Map.of(
                "transportation", sum(emissions.stream().map(CarbonEmission::getTransportationEmission).toList()),
                "electricity", sum(emissions.stream().map(CarbonEmission::getElectricityEmission).toList()),
                "food", sum(emissions.stream().map(CarbonEmission::getFoodEmission).toList()),
                "waste", sum(emissions.stream().map(CarbonEmission::getWasteEmission).toList())
        );

        return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

