package com.example.bankrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankrest.entity.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
}