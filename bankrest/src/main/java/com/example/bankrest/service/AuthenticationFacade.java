package com.example.bankrest.service;

import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {
  Authentication getAuthentication();

  String getAuthenticationName();
}