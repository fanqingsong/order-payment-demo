package com.example.orderservice.dto;

import com.example.orderservice.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(UUID id, String email, Role role, Instant createdAt) {
}
