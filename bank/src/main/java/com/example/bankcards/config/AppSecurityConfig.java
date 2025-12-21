package com.example.bankcards.config;

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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@EnableMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@Configuration
public class AppSecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {

    http
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
  public JwtAuthenticationConverter jwtAuthenticationConverter() {

    var rolesConverter = new JwtGrantedAuthoritiesConverter();

    rolesConverter.setAuthoritiesClaimName("roles"); // 👈 claim
    rolesConverter.setAuthorityPrefix("ROLE_"); // 👈 префикс

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      // Логируем весь токен или его части
      System.out.println("JWT Token Value: " + jwt.getTokenValue());
      System.out.println("JWT Claims: " + jwt.getClaims());
      return rolesConverter.convert(jwt);
    });
    return converter;
  }

  @Bean
  public GrantedAuthoritiesMapper userAuthoritiesMapper() {
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
  public CommonsRequestLoggingFilter logFilter() {
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
    filter.setIncludeHeaders(true); // Включает логирование заголовков (в т.ч. Authorization)
    filter.setIncludeQueryString(true);
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(10000);
    return filter;
  }

  @Bean
  public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
    return new DefaultOAuth2UserService();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    UserDetails user = User.withUsername("user")
        .password("{noop}password")
        .roles("USER", "ADMIN")
        .build();
    return new InMemoryUserDetailsManager(user);
  }
}