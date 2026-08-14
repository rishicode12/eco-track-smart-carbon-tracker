package com.ecotrack.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequest {

    @NotNull(message = "progressAdded is required")
    @Positive(message = "progressAdded must be greater than zero")
    private Double progressAdded;
}
