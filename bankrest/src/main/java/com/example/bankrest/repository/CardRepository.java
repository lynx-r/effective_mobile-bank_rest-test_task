package com.example.bankrest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankrest.entity.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
  Optional<Card> findByEmail(String email);
}