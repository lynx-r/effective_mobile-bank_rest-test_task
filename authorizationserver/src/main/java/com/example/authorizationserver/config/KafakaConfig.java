package com.example.authorizationserver.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafakaConfig {

  @Bean
  NewTopic userRegistrationTopic() {
    return TopicBuilder.name("user-registration-topic")
        .partitions(1)
        .replicas(1)
        .build();
  }
}
