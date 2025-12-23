package com.example.bankrest.dto;

public record CardholderResponse(
    Long id,
    String username,
    String firstName,
    String lastName,
    boolean enabled) {
}
