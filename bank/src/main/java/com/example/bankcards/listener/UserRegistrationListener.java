package com.example.bankcards.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.common.auth.event.UserCreatedEvent;

@Component
public class UserRegistrationListener {

  @KafkaListener(topics = "user-registration-topic", groupId = "bankcards-group")
  public void consume(UserCreatedEvent event) {
    System.out.println("BankCards получил сообщение из Kafka: " + event.username());
    // Здесь можно, например, создать пустую банковскую карту для нового
    // пользователя
  }
}
