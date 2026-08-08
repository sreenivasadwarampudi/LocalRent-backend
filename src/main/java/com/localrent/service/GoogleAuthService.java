package com.localrent.service;

import com.localrent.dto.AuthDtos.AuthResponse;

public interface GoogleAuthService {
    AuthResponse loginWithGoogle(String idToken);
}