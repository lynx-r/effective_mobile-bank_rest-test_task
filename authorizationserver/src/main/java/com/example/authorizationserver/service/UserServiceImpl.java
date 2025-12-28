package com.example.authorizationserver.service;

import java.time.LocalDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.authorizationserver.dto.RegisterRequest;
import com.example.authorizationserver.entity.User;
import com.example.authorizationserver.exception.UserAlreadyExistsException;
import com.example.authorizationserver.repository.RoleRepository;
import com.example.authorizationserver.repository.UserRepository;
import com.example.common.auth.event.UserCreatedEvent;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Override
  @Transactional
  public void register(RegisterRequest request) {
    boolean existsByUsername = userRepository.existsByUsername(request.username());
    if (existsByUsername) {
      throw new UserAlreadyExistsException("Пользователь уже существует");
    }

    boolean existsByEmail = userRepository.existsByEmail(request.email());
    if (existsByEmail) {
      throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
    }

    var user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    // Хешируем пароль перед сохранением!
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setCreatedAt(LocalDateTime.now());

    // Назначаем роль по умолчанию (ROLE_USER)
    var defaultRole = roleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new EntityNotFoundException("Роль ROLE_USER не найдена"));
    user.getRoles().add(defaultRole);

    userRepository.save(user);

    var event = new UserCreatedEvent(
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        LocalDateTime.now());
    kafkaTemplate.send("user-registration-topic", event);
    log.info("User registration event sent to Kafka: username={}", user.getUsername());
  }
}
