package com.example.bankcards.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankcards.service.CustomUserDetails;
import com.example.bankcards.service.CustomUserDetailsService;
import com.example.bankcards.service.TokenService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class AuthController {

  private final TokenService tokenService;
  private final AuthenticationManager authManager;
  private final CustomUserDetailsService userDetailsService;

  public AuthController(TokenService tokenService, AuthenticationManager authManager,
      CustomUserDetailsService userDetailsService) {
    super();
    this.tokenService = tokenService;
    this.authManager = authManager;
    this.userDetailsService = userDetailsService;
  }

  record LoginRequest(String username, String password) {
  };

  record LoginResponse(String message, String access_jwt_token, String refresh_jwt_token) {
  };

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request) {

    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.username,
        request.password);
    Authentication auth = authManager.authenticate(authenticationToken);

    CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByUsername(request.username);
    String access_token = tokenService.generateAccessToken(user);
    String refresh_token = tokenService.generateRefreshToken(user);

    return new LoginResponse("User with email = " + request.username + " successfully logined!", access_token,
        refresh_token);
  }

  record RefreshTokenResponse(String access_jwt_token, String refresh_jwt_token) {
  };

  @GetMapping("/token/refresh")
  public RefreshTokenResponse refreshToken(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");
    String refreshToken = headerAuth.substring(7, headerAuth.length());

    String email = tokenService.parseToken(refreshToken);
    CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
    String access_token = tokenService.generateAccessToken(user);
    String refresh_token = tokenService.generateRefreshToken(user);

    return new RefreshTokenResponse(access_token, refresh_token);
  }
}