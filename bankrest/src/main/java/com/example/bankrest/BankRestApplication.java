package com.example.bankrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.bankrest.config.CardConfig;
import com.example.bankrest.config.CryptoConfig;

@SpringBootApplication
@EnableConfigurationProperties({ CardConfig.class, CryptoConfig.class })
public class BankRestApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankRestApplication.class, args);
	}

}
