package com.localrent.controller;

import com.localrent.model.User;
import com.localrent.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record UpdatePhoneRequest(@NotBlank String phone) {}

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