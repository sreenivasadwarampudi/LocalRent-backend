package com.localrent.dto;

import com.localrent.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    /** Digits only, optionally prefixed with a country code. */
    public static final String PHONE_PATTERN = "^\\+?[0-9]{10,15}$";

    private AuthDtos() {
    }

    public record SignupRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = "Enter a valid phone number") String phone,
            @NotBlank @Size(min = 6, max = 100) String password) {
    }

    public record LoginRequest(
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = "Enter a valid phone number") String phone,
            @NotBlank String password) {
    }

    public record GoogleAuthRequest(
            @NotBlank String idToken) {
    }

    // Updated with the email field
    public record UserResponse(String id, String name, String phone, String email, Role role) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }
}