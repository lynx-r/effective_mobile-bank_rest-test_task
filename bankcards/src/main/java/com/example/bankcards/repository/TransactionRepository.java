package com.example.bankcards.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankcards.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
