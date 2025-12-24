package com.example.bankrest.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bankrest.entity.Card;

public interface CardRepository extends JpaRepository<Card, Long> {

  Page<Card> findByOwner_UsernameAndCardNumberMasked(
      String username,
      String search,
      Pageable pageable);

  @Query("SELECT c FROM Card c WHERE " +
      "(:search = '' OR " +
      "LOWER(c.ownerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "c.cardNumberMasked LIKE CONCAT('%', :search, '%'))")
  Page<Card> findByOwnerNameAndCardNumberMasked(@Param("search") String search, Pageable pageable);

  Optional<Card> findByIdAndOwner_Username(Long cardId, String username);

  Page<Card> findByOwner_Username(String username, Pageable pageable);
}
