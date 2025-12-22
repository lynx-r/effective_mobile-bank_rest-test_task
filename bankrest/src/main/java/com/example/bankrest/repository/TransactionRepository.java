package com.example.bankrest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bankrest.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
