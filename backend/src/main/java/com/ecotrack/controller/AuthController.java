package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.GoogleLoginRequest;
import com.ecotrack.dto.GoogleLoginResponse;
import com.ecotrack.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleAuthService googleAuthService;

    public AuthController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/google")
    @Operation(summary = "Authenticate with Google", description = "Verifies a Google ID token and returns an EcoTrack JWT.")
    @SecurityRequirements
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Google login successful",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired Google token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected authentication failure")
    })
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleLoginResponse responseData = googleAuthService.authenticateWithGoogle(request);
        ApiResponse<GoogleLoginResponse> response = new ApiResponse<>(true, "Google login successful", responseData);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}