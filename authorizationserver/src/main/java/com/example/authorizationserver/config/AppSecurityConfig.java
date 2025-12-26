package com.example.authorizationserver.config;

import java.util.List;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import com.example.authorizationserver.utils.Jwks;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class AppSecurityConfig {

  @Autowired
  private CorsProperties corsProperties;

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    http
        .cors(cors -> cors.configurationSource(request -> {
          CorsConfiguration config = new CorsConfiguration();
          config.setAllowedOrigins(corsProperties.getAllowedOrigins());
          config.setAllowedMethods(corsProperties.getAllowedMethods());
          config.setAllowedHeaders(List.of("*"));
          config.setAllowCredentials(corsProperties.isAllowCredentials());
          return config;
        }))
        .oauth2AuthorizationServer((authorizationServer) -> {
          http.securityMatcher(authorizationServer.getEndpointsMatcher());
          authorizationServer
              .oidc(oidc -> oidc.logoutEndpoint(Customizer.withDefaults()));
        })
        .authorizeHttpRequests((authorize) -> authorize
            .anyRequest().authenticated())
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
        .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/register"))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/auth/register").permitAll()
            .requestMatchers("/swagger-ui/**").permitAll()
            .requestMatchers("/v3/api-docs").permitAll()
            .requestMatchers("/openapi.yaml").permitAll()
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
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
        .issuer("http://auth-server:9000")
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  NewTopic userRegistrationTopic() {
    return TopicBuilder.name("user-registration-topic")
        .partitions(1)
        .replicas(1)
        .build();
  }
}
