package com.ecotrack.service;

import com.ecotrack.dto.GoogleLoginRequest;
import com.ecotrack.dto.GoogleLoginResponse;

public interface GoogleAuthService {

    GoogleLoginResponse authenticateWithGoogle(GoogleLoginRequest request);
}