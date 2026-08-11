package com.ecotrack.dto;

public class UserProfileUpdateRequest {
    private String fullName;
    private String role;
    private String location;
    private String commuteMode;
    private String dietPreference;
    private String country;
    private String interests;

    // Getters
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getLocation() { return location; }
    public String getCommuteMode() { return commuteMode; }
    public String getDietPreference() { return dietPreference; }
    public String getCountry() { return country; }
    public String getInterests() { return interests; }

    // Setters
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(String role) { this.role = role; }
    public void setLocation(String location) { this.location = location; }
    public void setCommuteMode(String commuteMode) { this.commuteMode = commuteMode; }
    public void setDietPreference(String dietPreference) { this.dietPreference = dietPreference; }
    public void setCountry(String country) { this.country = country; }
    public void setInterests(String interests) { this.interests = interests; }
}