package com.ecotrack.service;

import com.ecotrack.dto.ChangePasswordRequest;
import com.ecotrack.dto.LoginRequest;
import com.ecotrack.dto.LoginResponse;
import com.ecotrack.dto.UserProfileResponse;
import com.ecotrack.dto.UserRegistrationRequest;

public interface UserService {

    LoginResponse registerUser(UserRegistrationRequest request);

    LoginResponse loginUser(LoginRequest request);

    UserProfileResponse getUserProfile(String email);

    void changePassword(String email, ChangePasswordRequest request);

}