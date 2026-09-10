package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.ChangePasswordRequest;
import com.ecotrack.dto.LoginRequest;
import com.ecotrack.dto.LoginResponse;
import com.ecotrack.dto.UserRegistrationRequest;
import com.ecotrack.dto.UserProfileResponse;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.FileUploadService;
import com.ecotrack.service.UserService;
import com.ecotrack.dto.UserProfileUpdateRequest;

import jakarta.validation.Valid;

import com.ecotrack.service.RateLimitingService;
import com.ecotrack.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final RateLimitingService rateLimitingService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService,
                          UserRepository userRepository,
                          FileUploadService fileUploadService,
                          RateLimitingService rateLimitingService,
                          JwtUtil jwtUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.fileUploadService = fileUploadService;
        this.rateLimitingService = rateLimitingService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String clientIp = rateLimitingService.getClientIp(httpRequest);
        if (!rateLimitingService.tryConsume(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>(false, "Too many registration attempts. Please try again after 1 minute.", null));
        }

        LoginResponse registrationData = userService.registerUser(request);
        String accessToken = jwtUtil.generateAccessToken(registrationData.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(registrationData.getEmail());
        registrationData.setToken(accessToken);

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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String clientIp = rateLimitingService.getClientIp(httpRequest);
        if (!rateLimitingService.tryConsume(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>(false, "Too many login attempts. Please try again after 1 minute.", null));
        }

        if (!userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceNotFoundException("USER_NOT_FOUND");
        }

        LoginResponse loginData = userService.loginUser(request);
        String accessToken = jwtUtil.generateAccessToken(loginData.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(loginData.getEmail());
        loginData.setToken(accessToken);

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

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse profile = userService.getUserProfile(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile fetched successfully", profile));
    }

    // NAYA CLOUDINARY UPLOAD API
    @PostMapping("/profile-picture")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        try {
            // User ka email nikalenge jo login hai
            String email = authentication.getName();

            // Cloudinary par file upload karke URL lenge
            String imageUrl = fileUploadService.uploadFile(file);

            // Database mein user ko dhoondh kar uski image update karenge
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            user.setProfilePicture(imageUrl);
            userRepository.save(user);

            // Naya URL frontend ko bhej denge
            return ResponseEntity.ok(new ApiResponse<>(true, "Profile picture updated successfully", imageUrl));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Image upload failed: " + e.getMessage(), null));
        }
    }
    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @RequestBody UserProfileUpdateRequest request,
            Authentication authentication) {
        
        try {
            // Logged-in user ka email nikalein
            String email = authentication.getName();

            // Database mein user dhoondhein
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // Naya data set karein
            user.setFullName(request.getFullName());
            user.setRole(request.getRole());
            user.setLocation(request.getLocation());
            user.setCommuteMode(request.getCommuteMode());
            user.setDietPreference(request.getDietPreference());
            user.setCountry(request.getCountry());
            user.setInterests(request.getInterests());

            // Database mein save karein
            userRepository.save(user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully", null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to update profile: " + e.getMessage(), null));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            userService.changePassword(email, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Password changed successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userService.deactivateUser(user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Account deactivated successfully", null));
    }
}