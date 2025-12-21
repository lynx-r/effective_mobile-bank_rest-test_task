package com.example.bankrest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankrest.entity.Cardholder;

public interface CardholderRepository extends JpaRepository<Cardholder, Long> {
  Optional<Cardholder> findByEmail(String email);
}