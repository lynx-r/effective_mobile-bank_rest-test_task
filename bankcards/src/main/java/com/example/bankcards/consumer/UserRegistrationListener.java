package com.example.bankcards.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.bankcards.service.AdminCardholderService;
import com.example.common.auth.event.UserCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserRegistrationListener {

  private final AdminCardholderService cardholderService;

  @KafkaListener(topics = "user-registration-topic", groupId = "bankcards-group")
  public void consume(UserCreatedEvent event) {
    log.info("Received user registration event from Kafka: username={}, email={}",
        event.username(), event.email());
    cardholderService.registerCardholder(event);
  }
}
