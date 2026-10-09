package com.localrent.controller;

import com.localrent.dto.AuthDtos.AuthResponse;
import com.localrent.dto.AuthDtos.LoginRequest;
import com.localrent.dto.AuthDtos.SignupRequest;
import com.localrent.dto.AuthDtos.UserResponse;
import com.localrent.dto.FacebookAuthRequest;
import com.localrent.model.User;
import com.localrent.repository.UserRepository;
import com.localrent.dto.GoogleAuthRequest;
import com.localrent.service.AuthService;
import com.localrent.service.FacebookAuthService;
import com.localrent.service.GoogleAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;

    
    public record UpdatePhoneRequest(@NotBlank String phone) {}


    private final FacebookAuthService facebookAuthService;

    public AuthController(AuthService authService,
                          GoogleAuthService googleAuthService,
                          UserRepository userRepository,
                          FacebookAuthService facebookAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.userRepository = userRepository;
        this.facebookAuthService = facebookAuthService;
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
    
    @PostMapping("/facebook")
    public AuthResponse facebookLogin(@Valid @RequestBody FacebookAuthRequest request) {
        return facebookAuthService.loginWithFacebook(request.accessToken());
    }
    
    @PutMapping("/phone")
    public void updatePhone(Principal principal, @Valid @RequestBody UpdatePhoneRequest request) {
        User user = userRepository.findById(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String cleanPhone = request.phone().replaceAll("\\s+", "");

        // Check if phone is already taken by another user
        userRepository.findByPhone(cleanPhone).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number already in use");
            }
        });

        user.setPhone(cleanPhone);
        userRepository.save(user);
    }


    
}
