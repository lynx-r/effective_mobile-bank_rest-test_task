package com.example.bankcards.controller;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicController {
  @GetMapping("/token")
  public String getToken(@RegisteredOAuth2AuthorizedClient("oidc-client") OAuth2AuthorizedClient authorizedClient) {
    // Это тот самый токен, который нужно отправлять в заголовке Bearer
    String tokenValue = authorizedClient.getAccessToken().getTokenValue();
    System.out.println("Access Token: " + tokenValue);
    return tokenValue;
  }
}
