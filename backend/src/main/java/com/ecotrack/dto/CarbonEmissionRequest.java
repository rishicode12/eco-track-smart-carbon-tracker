package com.ecotrack.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CarbonEmissionRequest {

    @NotBlank(message = "Activity category is required")
    private String activityCategory;

    @NotNull(message = "CO2 impact is required")
    @DecimalMin(value = "0.00001", inclusive = true, message = "CO2 impact must be greater than or equal to 0.00001")
    private BigDecimal co2Impact;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    private BigDecimal waterConsumed;

    private String waterUnit; // "Liters" or "Gallons"

    private LocalDateTime loggedAt;
}