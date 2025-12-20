package com.example.bankcards.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {
  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String admin() {
    return "ADMIN OK";
  }

  @GetMapping("/user")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public String user() {
    return "USER OK";
  }

  @PreAuthorize("hasRole('USER')")
  @GetMapping("/me")
  public Map<String, Object> me(Authentication auth) {
    Jwt jwt = (Jwt) auth.getPrincipal();
    return jwt.getClaims();
  }
}
