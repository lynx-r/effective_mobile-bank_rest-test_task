package com.example.bankrest.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "card_number_encrypted", nullable = false)
  private String cardNumberEncrypted;

  @Column(name = "card_number_masked", length = 19, nullable = false)
  private String cardNumberMasked;

  @Column(name = "owner_name", length = 200, nullable = false)
  private String ownerName;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Builder.Default
  @Enumerated(EnumType.STRING) // Сохранять в БД как строку
  @Column(name = "status", length = 20, nullable = false)
  private CardStatus status = CardStatus.ACTIVE;

  @Builder.Default
  @Column(name = "balance", precision = 15, scale = 2, nullable = false)
  private BigDecimal balance = BigDecimal.ZERO;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cards_owner_id"))
  private Cardholder owner;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = true)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "fromCard")
  private List<Transaction> outgoingTransactions;

  @OneToMany(mappedBy = "toCard")
  private List<Transaction> incomingTransactions;
}
