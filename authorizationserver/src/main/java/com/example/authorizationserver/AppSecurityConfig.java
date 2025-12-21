package com.example.authorizationserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {

    // var authorizationServerConfigurer = new
    // OAuth2AuthorizationServerConfigurer();

    // authorizationServerConfigurer
    // .oidc(Customizer.withDefaults());

    // http

    // .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
    // .authorizeHttpRequests(authorize -> authorize
    // .anyRequest().authenticated())
    // .csrf(csrf -> csrf
    // .ignoringRequestMatchers(
    // authorizationServerConfigurer.getEndpointsMatcher()))
    // .exceptionHandling(exceptions -> exceptions
    // .defaultAuthenticationEntryPointFor(
    // new LoginUrlAuthenticationEntryPoint("/login"),
    // new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
    // .formLogin(Customizer.withDefaults())
    // .apply(authorizationServerConfigurer);

    http
        .oauth2AuthorizationServer((authorizationServer) -> {
          http.securityMatcher(authorizationServer.getEndpointsMatcher());
          authorizationServer
              .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0
        })
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
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
      throws Exception {

    http
        .authorizeHttpRequests(authorize -> authorize
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
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
  public UserDetailsService userDetailsService() {
    UserDetails user = User.withUsername("user")
        .password("{noop}password")
        .roles("USER", "ADMIN")
        .build();
    return new InMemoryUserDetailsManager(user);
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = Jwks.generateRsa();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }
}
