package com.localrent.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.localrent.dto.AuthDtos.AuthResponse;
import com.localrent.dto.AuthDtos.UserResponse;
import com.localrent.model.Role;
import com.localrent.model.User;
import com.localrent.repository.UserRepository;
import com.localrent.security.JwtService;
import com.localrent.service.GoogleAuthService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${google.client.id}")
    private String googleClientId;

    public GoogleAuthServiceImpl(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse loginWithGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Look up by email, create new user if not found
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setRole(Role.OWNER);
                return userRepository.save(newUser);
            });

            // Generate JWT matching your exact JwtService parameters
            String jwtToken = jwtService.generateToken(
                    user.getId(), 
                    user.getPhone(), 
                    user.getRole().name()
            );

            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getRole()
            );

            return new AuthResponse(jwtToken, userResponse);

        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }
}