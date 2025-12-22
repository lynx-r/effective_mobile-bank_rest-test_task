package com.example.authorizationserver.service;

import com.example.authorizationserver.dto.RegisterRequest;

public interface UserService {
  void register(RegisterRequest request);
}
