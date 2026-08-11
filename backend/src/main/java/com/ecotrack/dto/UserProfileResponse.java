package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;

    private String fullName;

    private String email;

    private String provider;

    private String profilePicture;

    private Integer rewardPoints;

    private String badgeName;

    private String role;

    private String location;

    private String commuteMode;

    private String dietPreference;

    private String country;

    private String interests;

}