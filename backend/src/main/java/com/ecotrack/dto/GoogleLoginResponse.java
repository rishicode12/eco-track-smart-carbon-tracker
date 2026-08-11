package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginResponse {

    private String token;
    private String message;
    private String email;
    private String fullName;
    private String provider;
    private String providerId;
    private String profilePicture;
    private Boolean emailVerified;
}