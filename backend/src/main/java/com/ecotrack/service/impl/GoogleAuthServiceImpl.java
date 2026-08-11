package com.ecotrack.service.impl;

import com.ecotrack.dto.GoogleLoginRequest;
import com.ecotrack.dto.GoogleLoginResponse;
import com.ecotrack.entity.User;
import com.ecotrack.exception.EmailAlreadyExistsException;
import com.ecotrack.exception.GoogleAuthenticationException;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.security.GoogleTokenVerifier;
import com.ecotrack.service.GoogleAuthService;
import com.ecotrack.utils.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public GoogleAuthServiceImpl(GoogleTokenVerifier googleTokenVerifier,
                                 UserRepository userRepository,
                                 JwtUtil jwtUtil) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GoogleLoginResponse authenticateWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());

        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new GoogleAuthenticationException("Google account email is missing");
        }

        String fullName = resolveFullName(payload);
        String googleUserId = payload.getSubject();
        String profilePicture = payload.get("picture") != null ? payload.get("picture").toString() : null;

        User user = userRepository.findByEmailIgnoreCase(email)
                .map(existingUser -> refreshGoogleProfile(existingUser, fullName, googleUserId, profilePicture, true))
                .orElseGet(() -> createGoogleUser(fullName, email, googleUserId, profilePicture));

        String jwt = jwtUtil.generateToken(user.getEmail());

        return new GoogleLoginResponse(
                jwt,
                "Google authentication successful",
                user.getEmail(),
                user.getFullName(),
                user.getProvider(),
                user.getProviderId(),
                user.getProfilePicture(),
                user.getEmailVerified()
        );
    }

    private User refreshGoogleProfile(User user,
                                      String fullName,
                                      String providerId,
                                      String profilePicture,
                                      boolean emailVerified) {
        if ("LOCAL".equalsIgnoreCase(user.getProvider())) {
            return user;
        }

        user.setFullName(fullName);
        user.setProvider("GOOGLE");
        user.setProviderId(providerId);
        user.setProfilePicture(profilePicture);
        user.setEmailVerified(emailVerified);
        return userRepository.save(user);
    }

    private User createGoogleUser(String fullName,
                                  String email,
                                  String providerId,
                                  String profilePicture) {
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(null)
                .provider("GOOGLE")
                .providerId(providerId)
                .profilePicture(profilePicture)
                .emailVerified(true)
                .build();

        return userRepository.save(user);
    }

    private String resolveFullName(GoogleIdToken.Payload payload) {
        Object fullName = payload.get("name");
        if (fullName != null && !Objects.toString(fullName).isBlank()) {
            return fullName.toString();
        }

        String fallbackName = payload.getEmail();
        return fallbackName != null ? fallbackName.split("@")[0] : "Google User";
    }
}