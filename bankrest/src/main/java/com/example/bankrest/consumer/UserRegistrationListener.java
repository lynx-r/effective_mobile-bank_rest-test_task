package com.example.bankrest.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.common.auth.event.UserCreatedEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserRegistrationListener {

  @KafkaListener(topics = "user-registration-topic", groupId = "bankrest-group")
  public void consume(UserCreatedEvent event) {
    log.info("Received user registration event from Kafka: username={}, email={}",
        event.username(), event.email());
    // Здесь можно, например, создать пустую банковскую карту для нового
    // пользователя
  }
}
