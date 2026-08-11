package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.CarbonEmissionRequest;
import com.ecotrack.dto.CarbonEmissionResponse;
import com.ecotrack.service.CarbonEmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carbon")
@RequiredArgsConstructor
public class CarbonEmissionController {

    private final CarbonEmissionService carbonEmissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CarbonEmissionResponse>> createEmission(
            @Valid @RequestBody CarbonEmissionRequest request,
            Authentication authentication
    ) {

        CarbonEmissionResponse response =
                carbonEmissionService.createEmission(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new ApiResponse<>(
                                true,
                                "Carbon activity logged successfully",
                                response
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarbonEmissionResponse>>> getEmissions(
            Authentication authentication
    ) {

        List<CarbonEmissionResponse> response =
                carbonEmissionService.getUserEmissions(
                        authentication.getName()
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Carbon activities fetched successfully",
                        response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CarbonEmissionResponse>> updateEmission(
            @PathVariable Long id,
            @Valid @RequestBody CarbonEmissionRequest request,
            Authentication authentication
    ) {

        CarbonEmissionResponse response =
                carbonEmissionService.updateEmission(
                        id,
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Carbon activity updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmission(
            @PathVariable Long id,
            Authentication authentication
    ) {

        carbonEmissionService.deleteEmission(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Carbon activity deleted successfully",
                        null
                )
        );
    }
}