package com.ecotrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Pattern;

@Data
public class UserRegistrationRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@(gmail\\.com|outlook\\.com)$", 
             message = "Security Error: Only @gmail.com or @outlook.com accounts are allowed to register.")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String country;

}
