package com.example.authorizationserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authorizationserver.dto.RegisterRequest;
import com.example.authorizationserver.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
    userService.register(request);
    return ResponseEntity.ok("Пользователь успешно зарегистрирован");
  }
}
