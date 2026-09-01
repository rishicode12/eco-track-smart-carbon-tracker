package com.ecotrack.service;

public interface EmailService {

    void sendPasswordResetEmail(String to, String token);
}
