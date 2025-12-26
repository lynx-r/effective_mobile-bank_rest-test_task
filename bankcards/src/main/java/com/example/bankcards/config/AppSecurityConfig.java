package com.example.bankcards.config;

import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@EnableMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@Configuration
public class AppSecurityConfig {

  @Autowired
  private ClientRegistrationRepository clientRegistrationRepository;
  @Autowired
  private CorsProperties corsProperties;

  @Bean
  @Order(1)
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**")
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/public/**"))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/public/**").permitAll()
            .anyRequest().authenticated())
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userAuthoritiesMapper(userAuthoritiesMapper())))
        .oauth2Client(Customizer.withDefaults())
        .logout(logout -> logout
            .logoutSuccessHandler(oidcLogoutSuccessHandler()));
    return http.build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    return request -> {
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(corsProperties.getAllowedOrigins());
      config.setAllowedMethods(corsProperties.getAllowedMethods());
      config.setAllowedHeaders(List.of("*"));
      config.setAllowCredentials(corsProperties.isAllowCredentials());
      return config;
    };
  }

  private LogoutSuccessHandler oidcLogoutSuccessHandler() {
    var successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    successHandler.setPostLogoutRedirectUri("http://client-app:8080/login?logout");
    return successHandler;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    var rolesConverter = new JwtGrantedAuthoritiesConverter();
    rolesConverter.setAuthoritiesClaimName("roles");
    rolesConverter.setAuthorityPrefix("ROLE_");

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