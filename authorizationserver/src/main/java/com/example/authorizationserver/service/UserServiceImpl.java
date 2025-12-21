package com.example.authorizationserver.service;

import java.time.LocalDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.authorizationserver.entity.User;
import com.example.authorizationserver.repository.RoleRepository;
import com.example.authorizationserver.repository.UserRepository;
import com.example.authorizationserver.request.RegisterRequest;
import com.example.common.auth.event.UserCreatedEvent;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository; // Вам понадобится репозиторий для ролей
  private final PasswordEncoder passwordEncoder;
  private final KafkaTemplate<String, Object> kafkaTemplate; // Шаблон для отправки

  @Transactional
  public void register(RegisterRequest request) {
    if (userRepository.findByUsername(request.username()).isPresent()) {
      throw new RuntimeException("Пользователь уже существует");
    }

    var user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    // Хешируем пароль перед сохранением!
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setCreatedAt(LocalDateTime.now());

    // Назначаем роль по умолчанию (ROLE_USER)
    var defaultRole = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new RuntimeException("Роль ROLE_USER не найдена"));
    user.getRoles().add(defaultRole);

    userRepository.save(user);

    var event = new UserCreatedEvent(
        user.getUsername(),
        "example@mail.com", // если есть email
        LocalDateTime.now());
    kafkaTemplate.send("user-registration-topic", event);
    System.out.println("Sent to Kafka: " + event);
  }
}
