package com.example.authorizationserver.service;

import com.example.authorizationserver.request.RegisterRequest;

public interface UserService {
  void register(RegisterRequest request);
}
