package com.example.bankcards.controller;

import java.util.Map;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {

  /**
   * Отдает токен пользователю авторизованном через /login
   * 
   * @param client
   * @return
   */
  @GetMapping("/api/token")
  public Map<String, String> getToken(@RegisteredOAuth2AuthorizedClient("oidc-client") OAuth2AuthorizedClient client) {
    return Map.of(
        "access_token", client.getAccessToken().getTokenValue(),
        "refresh_token", client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : "none");
  }
}
