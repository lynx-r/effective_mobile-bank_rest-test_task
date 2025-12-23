package com.example.bankrest.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "cardholders")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cardholder {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", unique = true)
  private String username;

  @Column(name = "email", unique = true)
  private String email;

  @NotNull
  @NotBlank
  @Column(name = "first_name")
  private String firstName;

  @NotNull
  @NotBlank
  @Column(name = "last_name")
  private String lastName;

  @Builder.Default
  @Column(name = "enabled")
  private Boolean enabled = true;

  @Builder.Default
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = true)
  private LocalDateTime updatedAt;

  @Builder.Default
  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
  private List<Card> cards = new ArrayList<>();

  public void addCard(Card card) {
    if (cards == null) {
      cards = new ArrayList<>();
    }
    cards.add(card);
    card.setOwner(this);
  }

  public String getCardOwnerName() {
    if (getFirstName() == null || getLastName() == null) {
      return "";
    }
    return getFirstName().toUpperCase() + " " + getLastName().toUpperCase();
  }
}
