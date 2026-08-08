package com.localrent.controller;

import com.localrent.dto.AuthDtos.AuthResponse;
import com.localrent.dto.AuthDtos.LoginRequest;
import com.localrent.dto.AuthDtos.SignupRequest;
import com.localrent.dto.AuthDtos.UserResponse;
import com.localrent.dto.GoogleAuthRequest;
import com.localrent.service.AuthService;
import com.localrent.service.GoogleAuthService;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return authService.currentUser(principal.getName());
    }
    
    @PostMapping("/google")
    public AuthResponse googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        return googleAuthService.loginWithGoogle(request.idToken());
    }

    
}
