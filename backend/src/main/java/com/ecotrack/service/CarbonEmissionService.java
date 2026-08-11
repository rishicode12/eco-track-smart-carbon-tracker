package com.ecotrack.service;

import com.ecotrack.dto.CarbonEmissionRequest;
import com.ecotrack.dto.CarbonEmissionResponse;

import java.util.List;

public interface CarbonEmissionService {

    CarbonEmissionResponse createEmission(
            String email,
            CarbonEmissionRequest request
    );

    List<CarbonEmissionResponse> getUserEmissions(
            String email
    );

    CarbonEmissionResponse updateEmission(
            Long id,
            String email,
            CarbonEmissionRequest request
    );

    void deleteEmission(
            Long id,
            String email
    );
}