package com.ecotrack.security;

import com.ecotrack.entity.User;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.utils.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String email = oidcUser.getEmail();
        String fullName = resolveFullName(oidcUser);
        String googleId = oidcUser.getSubject();
        String profilePicture = oidcUser.getPicture() != null ? oidcUser.getPicture() : null;

        User existingUser = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (existingUser != null && Boolean.FALSE.equals(existingUser.getIsActive())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This account has been deactivated. Please contact support.");
            return;
        }

        User user = existingUser != null
                ? refreshGoogleProfile(existingUser, fullName, googleId, profilePicture)
                : createGoogleUser(fullName, email, googleId, profilePicture);

        String jwt = jwtUtil.generateToken(user.getEmail());

        response.sendRedirect(frontendUrl + "/auth?token=" + jwt);
    }

    private User refreshGoogleProfile(User user, String fullName, String providerId, String profilePicture) {
        if ("LOCAL".equalsIgnoreCase(user.getProvider())) {
            return user;
        }
        user.setFullName(fullName);
        user.setProvider("GOOGLE");
        user.setProviderId(providerId);
        user.setProfilePicture(profilePicture);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private User createGoogleUser(String fullName, String email, String providerId, String profilePicture) {
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

    private String resolveFullName(OidcUser oidcUser) {
        String name = oidcUser.getFullName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        String email = oidcUser.getEmail();
        return email != null ? email.split("@")[0] : "Google User";
    }
}
