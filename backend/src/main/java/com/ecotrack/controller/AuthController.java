package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.ForgotPasswordRequest;
import com.ecotrack.dto.GoogleLoginRequest;
import com.ecotrack.dto.GoogleLoginResponse;
import com.ecotrack.dto.ResetPasswordRequest;
import com.ecotrack.entity.PasswordResetToken;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.PasswordResetTokenRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.EmailService;
import com.ecotrack.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public AuthController(GoogleAuthService googleAuthService,
                          UserRepository userRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          EmailService emailService,
                          BCryptPasswordEncoder passwordEncoder,
                          JdbcTemplate jdbcTemplate) {
        this.googleAuthService = googleAuthService;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint", description = "Pings database and returns status to keep server and database awake.")
    @SecurityRequirements
    public ResponseEntity<String> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok("Backend and Database are UP!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Database connection failed: " + e.getMessage());
        }
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

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Generates a reset token and sends a password reset email.")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email address"));

        if ("GOOGLE".equalsIgnoreCase(user.getProvider())) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "This account uses Google sign-in. Password reset is not available.", null));
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), token);

        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset email sent successfully", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Validates the reset token and updates the password.")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "This reset token has already been used", null));
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Reset token has expired. Please request a new one", null));
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successful", null));
    }
}
