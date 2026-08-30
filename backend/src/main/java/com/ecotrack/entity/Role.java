package com.ecotrack.entity;

/**
 * Application roles. The User entity stores the role as a String, but the
 * canonical set is defined here. "ADMIN" (and the Spring convention
 * "ROLE_ADMIN") both map to the administrator role.
 */
public enum Role {
    USER,
    ADMIN;

    public static Role fromString(String role) {
        if (role == null) {
            return USER;
        }
        if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return ADMIN;
        }
        return USER;
    }
}