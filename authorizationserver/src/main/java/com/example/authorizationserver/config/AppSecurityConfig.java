package com.example.authorizationserver.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import com.example.authorizationserver.utils.Jwks;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class AppSecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {

    http
        .oauth2AuthorizationServer((authorizationServer) -> {
          http.securityMatcher(authorizationServer.getEndpointsMatcher());
          authorizationServer
              .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0
        })
        // настройте его)
        .authorizeHttpRequests((authorize) -> authorize
            .anyRequest().authenticated())
        // Redirect to the login page when not authenticated from the
        // authorization endpoint
        .exceptionHandling((exceptions) -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
      throws Exception {

    http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/register")) // Отключаем CSRF только для регистрации
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/auth/register").permitAll() // Разрешаем доступ всем
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
    return context -> {

      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
          || "id_token".equals(context.getTokenType().getValue())) {

        var principal = context.getPrincipal();

        if (principal.getPrincipal() instanceof UserDetails user) {

          var roles = user.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .filter(a -> a.startsWith("ROLE_"))
              .map(a -> a.substring(5)) // ROLE_ADMIN → ADMIN
              .toList();

          context.getClaims().claims((claims) -> {
            claims.put("roles", roles);
            claims.put("test_field", "server_works");
          });
        }
      }
    };
  }

  @Bean
  JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = Jwks.generateRsa();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }

  @Bean
  NewTopic userRegistrationTopic() {
    return TopicBuilder.name("user-registration-topic")
        .partitions(1)
        .replicas(1)
        .build();
  }
}
