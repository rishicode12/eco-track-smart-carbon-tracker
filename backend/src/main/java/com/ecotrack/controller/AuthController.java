package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.ForgotPasswordRequest;
import com.ecotrack.dto.GoogleLoginRequest;
import com.ecotrack.dto.GoogleLoginResponse;
import com.ecotrack.dto.LoginRequest;
import com.ecotrack.dto.LoginResponse;
import com.ecotrack.dto.ResetPasswordRequest;
import com.ecotrack.dto.UserRegistrationRequest;
import com.ecotrack.entity.PasswordResetToken;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.PasswordResetTokenRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.EmailService;
import com.ecotrack.service.GoogleAuthService;
import com.ecotrack.service.RateLimitingService;
import com.ecotrack.service.UserService;
import com.ecotrack.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
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
    private final UserService userService;
    private final RateLimitingService rateLimitingService;
    private final JwtUtil jwtUtil;

    public AuthController(GoogleAuthService googleAuthService,
                          UserRepository userRepository,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          EmailService emailService,
                          BCryptPasswordEncoder passwordEncoder,
                          JdbcTemplate jdbcTemplate,
                          UserService userService,
                          RateLimitingService rateLimitingService,
                          JwtUtil jwtUtil) {
        this.googleAuthService = googleAuthService;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.rateLimitingService = rateLimitingService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user with email and password, applies IP rate limiting, and issues access token with HttpOnly refresh cookie.")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        // 1. IP Rate Limiting (5 requests/minute)
        String clientIp = rateLimitingService.getClientIp(httpRequest);
        if (!rateLimitingService.tryConsume(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>(false, "Too many login attempts. Please try again after 1 minute.", null));
        }

        // 2. Check if email exists before password check
        if (!userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceNotFoundException("USER_NOT_FOUND");
        }

        // 3. Authenticate user
        LoginResponse loginData = userService.loginUser(request);

        // 4. Generate 15-min Access Token & 7-day Refresh Token
        String accessToken = jwtUtil.generateAccessToken(loginData.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(loginData.getEmail());
        loginData.setToken(accessToken);

        // 5. Attach Refresh Token as HttpOnly, Secure cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        ApiResponse<LoginResponse> response = new ApiResponse<>(true, "Login successful", loginData);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Registers a new user with IP rate limiting and issues access token with HttpOnly refresh cookie.")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        // 1. IP Rate Limiting (5 requests/minute)
        String clientIp = rateLimitingService.getClientIp(httpRequest);
        if (!rateLimitingService.tryConsume(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>(false, "Too many registration attempts. Please try again after 1 minute.", null));
        }

        // 2. Register user
        LoginResponse registrationData = userService.registerUser(request);

        // 3. Generate 15-min Access Token & 7-day Refresh Token
        String accessToken = jwtUtil.generateAccessToken(registrationData.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(registrationData.getEmail());
        registrationData.setToken(accessToken);

        // 4. Attach Refresh Token as HttpOnly, Secure cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        ApiResponse<LoginResponse> response = new ApiResponse<>(true, "User registered successfully", registrationData);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Validates HttpOnly refresh token cookie and issues a fresh 15-minute access token.")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {

        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid or expired refresh token", null));
        }

        String email = jwtUtil.extractEmail(refreshToken);
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "User not found", null));
        }

        // Issue fresh access token and rotate refresh token
        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        Map<String, String> data = Map.of(
                "token", newAccessToken,
                "email", email
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Token refreshed successfully", data));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Clears the HttpOnly refresh token cookie.")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse httpResponse) {
        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out successfully", null));
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
