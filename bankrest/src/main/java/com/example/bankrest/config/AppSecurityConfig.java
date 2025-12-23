package com.example.bankrest.config;

import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@EnableMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@Configuration
public class AppSecurityConfig {

  @Autowired
  private ClientRegistrationRepository clientRegistrationRepository;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {

    http
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**"))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/public/**").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userAuthoritiesMapper(userAuthoritiesMapper()) // ПОДКЛЮЧИТЕ МАППЕР
            ))
        .oauth2Client(Customizer.withDefaults())
        .logout(logout -> logout
            .logoutSuccessHandler(oidcLogoutSuccessHandler()) // Специальный хендлер
        );

    return http.build();
  }

  private LogoutSuccessHandler oidcLogoutSuccessHandler() {
    OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(
        clientRegistrationRepository);

    successHandler.setPostLogoutRedirectUri("http://client-app:8080/login?logout");
    return successHandler;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    var rolesConverter = new JwtGrantedAuthoritiesConverter();
    rolesConverter.setAuthoritiesClaimName("roles");
    rolesConverter.setAuthorityPrefix("");

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
    return converter;
  }

  @Bean
  GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return (authorities) -> {
      var mappedAuthorities = new HashSet<GrantedAuthority>();
      authorities.forEach(authority -> {
        mappedAuthorities.add(authority);
        if (authority instanceof OidcUserAuthority oidcAuth) {
          List<String> roles = oidcAuth.getIdToken().getClaim("roles");
          if (roles != null) {
            roles.forEach(role -> mappedAuthorities.add(
                new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
          }
        }
      });
      return mappedAuthorities;
    };
  }

}