package com.example.bankrest.service;

import java.util.List;

import com.example.bankrest.dto.UserResponse;

public interface UserService {
  List<UserResponse> findAllUsers();

  void blockUser(Long id);

  void deleteUser(Long id);
}
