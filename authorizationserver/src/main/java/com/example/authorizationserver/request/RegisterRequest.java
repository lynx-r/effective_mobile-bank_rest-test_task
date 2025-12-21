package com.example.authorizationserver.request;

public record RegisterRequest(String username, String email, String password) {
}
