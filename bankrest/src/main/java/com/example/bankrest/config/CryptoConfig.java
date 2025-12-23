package com.example.bankrest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(prefix = "bank.crypto")
@Getter
@Setter
@Validated
@RequiredArgsConstructor
@Slf4j
public class CryptoConfig {

  @NotBlank(message = "Encryption key must not be blank")
  @Size(min = 32, max = 32, message = "Encryption key must be exactly 32 characters for AES-256")
  private String encryptionKey;

  @PostConstruct
  public void validateEncryptionKey() {
    if (encryptionKey == null || encryptionKey.length() != 32) {
      log.error("Invalid encryption key configuration. Key must be exactly 32 characters for AES-256 encryption.");
      throw new IllegalStateException("Invalid encryption key: must be exactly 32 characters for AES-256 encryption");
    }

    // Проверяем, что ключ не является дефолтным или слабым
    if (encryptionKey.equals("12345678123456781234567812345678") ||
        encryptionKey.matches("^a+$") ||
        encryptionKey.matches("^0+$")) {
      log.warn("Weak encryption key detected. Consider using a cryptographically secure key.");
    }

    log.info("Encryption key validation passed. Key length: {} characters", encryptionKey.length());
  }
}
