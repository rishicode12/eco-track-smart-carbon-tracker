package com.ecotrack.service.impl;

import com.ecotrack.dto.ChangePasswordRequest;
import com.ecotrack.dto.LoginRequest;
import com.ecotrack.dto.LoginResponse;
import com.ecotrack.dto.UserProfileResponse;
import com.ecotrack.dto.UserRegistrationRequest;
import com.ecotrack.entity.User;
import com.ecotrack.exception.EmailAlreadyExistsException;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.UserService;
import com.ecotrack.utils.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public LoginResponse registerUser(UserRegistrationRequest request) {

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .country(request.getCountry())
                .provider("LOCAL")
                .emailVerified(false)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser.getEmail());

        return new LoginResponse(
                token,
                "Registration Successful",
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getProvider(),
                savedUser.getProfilePicture()
        );
    }

    @Override
    public LoginResponse loginUser(LoginRequest request) {

        if (!userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResourceNotFoundException("USER_NOT_FOUND");
        }

        User user = userRepository.findByEmailIgnoreCaseAndIsActive(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("This account has been deactivated. Please contact support.");
        }

        if ("GOOGLE".equalsIgnoreCase(user.getProvider())) {
            throw new RuntimeException("This account uses Google sign-in. Please continue with Google.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        return new LoginResponse(
                token,
                "Login Successful",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getProvider(),
                user.getProfilePicture()
        );
    } 

    @Override
    public UserProfileResponse getUserProfile(String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getProvider(),
                user.getProfilePicture(),
                user.getRewardPoints(),
                user.getBadgeName(),
                user.getRole(),
                user.getLocation(),
                user.getCommuteMode(),
                user.getDietPreference(),
                user.getCountry(),
                user.getInterests()
        );
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("GOOGLE".equalsIgnoreCase(user.getProvider())) {
            throw new RuntimeException("This account uses Google sign-in. Password cannot be changed.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }
}