package com.example.orderservice.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt) {
}
