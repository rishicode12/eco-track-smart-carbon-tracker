package com.ecotrack.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeCompletionRequest {

    @NotNull(message = "Challenge id is required")
    private Long challengeId;
}

