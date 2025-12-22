package com.example.bankrest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "bank.crypto")
@Getter
@Setter
public class CryptoConfig {
  private String encryptionKey = "12345678123456781234567812345678";
}

