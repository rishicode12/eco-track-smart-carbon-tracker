package com.ecotrack.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // Naye fields add karein
    private String role;
    private String location;
    private String commuteMode;
    private String dietPreference;
    private String country;
    private String interests;

    // Agar Lombok (@Data) use nahi kar rahe, toh inke Getters/Setters bhi add karein:
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getCommuteMode() { return commuteMode; }
    public void setCommuteMode(String commuteMode) { this.commuteMode = commuteMode; }
    
    public String getDietPreference() { return dietPreference; }
    public void setDietPreference(String dietPreference) { this.dietPreference = dietPreference; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getInterests() { return interests; }
    public void setInterests(String interests) { this.interests = interests; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private String provider = "LOCAL";

    private String providerId;

    private String profilePicture;

    @Builder.Default
    private Boolean emailVerified = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer rewardPoints = 0;

    @Builder.Default
    @Column(nullable = false)
    private String badgeName = "Bronze";
}
