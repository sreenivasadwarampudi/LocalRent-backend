package com.localrent.service;

import com.localrent.dto.AuthDtos.AuthResponse;
import com.localrent.dto.AuthDtos.UserResponse;
import com.localrent.model.Role;
import com.localrent.model.User;
import com.localrent.repository.UserRepository;
import com.localrent.security.JwtService;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FacebookAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestTemplate rest = new RestTemplate();

    @Value("${facebook.app-id}")
    private String appId;

    @Value("${facebook.app-secret}")
    private String appSecret;

    public FacebookAuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @SuppressWarnings("unchecked")
    public AuthResponse loginWithFacebook(String accessToken) {
        String facebookId;
        String name;
        String email;

        try {
            // 1. Verify the token is valid and was issued for OUR app
            URI debugUri = UriComponentsBuilder.fromUriString("https://graph.facebook.com/debug_token")
                    .queryParam("input_token", accessToken)
                    .queryParam("access_token", appId + "|" + appSecret)
                    .build().toUri();
            Map<String, Object> debug = rest.getForObject(debugUri, Map.class);
            Map<String, Object> data = debug == null ? null : (Map<String, Object>) debug.get("data");

            if (data == null
                    || !Boolean.TRUE.equals(data.get("is_valid"))
                    || !appId.equals(String.valueOf(data.get("app_id")))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Facebook token");
            }

            // 2. Fetch the profile
            URI meUri = UriComponentsBuilder.fromUriString("https://graph.facebook.com/me")
                    .queryParam("fields", "id,name,email")
                    .queryParam("access_token", accessToken)
                    .build().toUri();
            Map<String, Object> me = rest.getForObject(meUri, Map.class);
            if (me == null || me.get("id") == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Could not read Facebook profile");
            }
            facebookId = String.valueOf(me.get("id"));
            name = (String) me.get("name");
            email = (String) me.get("email"); // may be null
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Facebook login failed");
        }

        // 3. Find by facebookId, then by email, otherwise create a new user
        final String emailFinal = email;
        User user = userRepository.findByFacebookId(facebookId)
                .or(() -> (emailFinal == null || emailFinal.isBlank())
                        ? Optional.<User>empty()
                        : userRepository.findByEmail(emailFinal))
                .orElse(null);

        if (user == null) {
            user = new User();
            user.setName(name != null ? name : "Facebook User");
            if (email != null && !email.isBlank()) {
                user.setEmail(email);
            }
            user.setRole(Role.OWNER);
        }
        if (user.getFacebookId() == null) {
            user.setFacebookId(facebookId);
        }
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getPhone(), user.getRole().name());
        return new AuthResponse(token, new UserResponse(
                user.getId(), user.getName(), user.getPhone(), user.getEmail(), user.getRole()));
    }
}