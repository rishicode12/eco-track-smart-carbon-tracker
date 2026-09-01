package com.ecotrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    private String email;
}
