package com.ecotrack.security;

import com.ecotrack.exception.GoogleAuthenticationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleTokenVerifier(GoogleIdTokenVerifier googleIdTokenVerifier) {
        this.googleIdTokenVerifier = googleIdTokenVerifier;
    }

    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(idToken);

            if (googleIdToken == null) {
                throw new GoogleAuthenticationException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new GoogleAuthenticationException("Google email address is not verified");
            }

            return payload;
        } catch (GoogleAuthenticationException ex) {
            throw ex;
        } catch (GeneralSecurityException ex) {
            throw new GoogleAuthenticationException("Google token verification failed");
        } catch (IOException ex) {
            throw new GoogleAuthenticationException("Google token verification failed");
        }
    }
}