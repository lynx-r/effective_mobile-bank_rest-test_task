package com.example.bankcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.bankcards.config.CardConfig;
import com.example.bankcards.config.CryptoConfig;

@SpringBootApplication
@EnableConfigurationProperties({ CardConfig.class, CryptoConfig.class })
public class BankCardsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankCardsApplication.class, args);
	}

}
