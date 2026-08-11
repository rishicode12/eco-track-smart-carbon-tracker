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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    // Yahan humne fileUploadService aur userRepository ko constructor mein add kar diya hai
    public UserController(UserService userService, UserRepository userRepository, FileUploadService fileUploadService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        LoginResponse registrationData = userService.registerUser(request);
        
        ApiResponse<LoginResponse> response = new ApiResponse<>(true, "User registered successfully", registrationData);
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginData = userService.loginUser(request);
        
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
}