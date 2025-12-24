package com.example.bankcards.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Карта отправителя
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_from_card_id"))
  private Card fromCard;

  // Карта получателя
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transactions_to_card_id"))
  private Card toCard;

  @Column(name = "amount", precision = 15, scale = 2, nullable = false)
  private BigDecimal amount;

  @Column(name = "description")
  private String description;

  @Builder.Default
  @Column(name = "status", length = 20, nullable = false)
  private String status = "COMPLETED";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
