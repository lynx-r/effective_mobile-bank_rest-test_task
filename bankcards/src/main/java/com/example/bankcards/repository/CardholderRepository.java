package com.example.bankcards.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bankcards.entity.Cardholder;

public interface CardholderRepository extends JpaRepository<Cardholder, Long> {

  @Query("SELECT c FROM Cardholder c WHERE " +
      "(:search = '' OR " +
      "LOWER(c.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Cardholder> findByUserInfo(@Param("search") String search, Pageable pageable);

  Optional<Cardholder> findByEmail(String email);
}