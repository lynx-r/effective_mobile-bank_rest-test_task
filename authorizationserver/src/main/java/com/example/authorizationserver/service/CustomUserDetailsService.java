package com.example.authorizationserver.service;

import static org.springframework.security.core.userdetails.User.builder;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.authorizationserver.entity.User;
import com.example.authorizationserver.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .toList())
        .build();
  }
}
