package com.example.bankrest.config;

import java.util.HashSet;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@EnableMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@Configuration
public class AppSecurityConfig {

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
        .oauth2Login(Customizer.withDefaults())
        .oauth2Client(Customizer.withDefaults());

    return http.build();
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
        mappedAuthorities.add(authority); // Сохраняем OIDC_USER и SCOPE_

        if (authority instanceof OidcUserAuthority oidcAuth) {
          // Извлекаем все атрибуты (claims) из токена
          var attributes = oidcAuth.getAttributes();

          // Допустим, ваш сервер авторизации кладет роли в список "groups"
          @SuppressWarnings("unchecked")
          var roles = (List<String>) attributes.get("roles");

          if (roles != null) {
            roles.forEach(role -> {
              // Маппим группы из токена в роли Spring Security
              // Например, если в токене группа "ADMIN_GROUP", делаем ROLE_ADMIN
              mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            });
          }
        }
      });

      return mappedAuthorities;
    };
  }

  @Bean
  OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
    return new DefaultOAuth2UserService();
  }
}