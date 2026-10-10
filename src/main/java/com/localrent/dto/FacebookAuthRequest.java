package com.localrent.dto;

import jakarta.validation.constraints.NotBlank;

public record FacebookAuthRequest(@NotBlank String accessToken) {}