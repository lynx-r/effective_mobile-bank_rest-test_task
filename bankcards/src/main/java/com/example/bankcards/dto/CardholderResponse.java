package com.example.bankcards.dto;

public record CardholderResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled) {
}
