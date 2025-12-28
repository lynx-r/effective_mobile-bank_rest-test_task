package com.example.bankcards.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.common.auth.event.RequestBlockCardEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlockCardListener {

  @KafkaListener(topics = "block-card-topic", groupId = "bankcards-group")
  public void consume(RequestBlockCardEvent event) {
    log.info("Received request block card event from Kafka: cardId={}, ownerId={}",
        event.cardId(), event.ownerId());
  }
}
