package com.example.bankcards.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Конфигурация OpenAPI 3 для банковской REST API системы
 */
@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI bankApi() {
    return new OpenAPI()
        .info(apiInfo())
        .servers(servers())
        .addSecurityItem(securityRequirement())
        .components(securityComponents());
  }

  private Info apiInfo() {
    return new Info()
        .title("Банковская REST API система")
        .description("""
            Полная документация API банковской системы для управления картами и пользователями.

            ## Основные возможности:
            - Регистрация и аутентификация пользователей
            - Управление банковскими картами
            - Переводы между картами
            - Административные функции

            ## Безопасность:
            - JWT аутентификация
            - Ролевая модель доступа (USER/ADMIN)
            - Шифрование чувствительных данных

            ## Аутентификация:
            Все endpoints (кроме регистрации) требуют Bearer токен в заголовке:
            ```
            Authorization: Bearer {jwt_token}
            ```
            """)
        .version("1.0.0")
        .contact(contactInfo())
        .license(licenseInfo());
  }

  private Contact contactInfo() {
    return new Contact()
        .name("API Support")
        .email("support@bank.com")
        .url("https://bank.com/support");
  }

  private License licenseInfo() {
    return new License()
        .name("MIT")
        .url("https://opensource.org/licenses/MIT");
  }

  private List<Server> servers() {
    return List.of(
        new Server()
            .url("http://client-app:8080")
            .description("Локальный сервер разработки"));
  }

  private SecurityRequirement securityRequirement() {
    return new SecurityRequirement()
        .addList("Bearer Authentication");
  }

  private Components securityComponents() {
    return new Components()
        .addSecuritySchemes("Bearer Authentication", securityScheme());
  }

  private SecurityScheme securityScheme() {
    return new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .description("JWT токен для аутентификации")
        .name("Authorization");
  }
}
