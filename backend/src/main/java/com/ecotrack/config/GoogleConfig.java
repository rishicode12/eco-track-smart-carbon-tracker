package com.ecotrack.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GoogleConfig {

    @Bean
    public NetHttpTransport googleHttpTransport() {
        return new NetHttpTransport();
    }

    @Bean
    public JsonFactory googleJsonFactory() {
        return GsonFactory.getDefaultInstance();
    }

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(NetHttpTransport googleHttpTransport,
                                                        JsonFactory googleJsonFactory,
                                                        @Value("${google.client.id}") String googleClientId) {
        return new GoogleIdTokenVerifier.Builder(googleHttpTransport, googleJsonFactory)
                .setAudience(List.of(googleClientId))
                .setIssuers(List.of("accounts.google.com", "https://accounts.google.com"))
                .build();
    }
}