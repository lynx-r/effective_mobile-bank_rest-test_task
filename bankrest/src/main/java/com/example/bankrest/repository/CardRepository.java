package com.example.bankrest.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bankrest.entity.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
  // Поиск по владельцу + фильтр по части номера или имени владельца (ownerName)
  @Query("SELECT c FROM Card c WHERE c.owner.username = :username " +
      "AND (:search IS NULL OR c.cardNumberMasked LIKE CONCAT('%', :search, '%') " +
      "OR LOWER(c.ownerName) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<Card> findByUserWithFilter(@Param("username") String username,
      @Param("search") String search,
      Pageable pageable);

  Optional<Card> findByIdAndOwner_Username(Long cardId, String username);

  Page<Card> findByOwner_Username(String username, Pageable pageable);
}