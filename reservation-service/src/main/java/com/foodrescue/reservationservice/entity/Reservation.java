package com.foodrescue.reservationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // AJOUT : consumerId (au lieu de userId)
    // ==========================================
    @Column(nullable = false)
    private Long consumerId;

    @Column(nullable = false)
    private Long offerId;

    @Column(nullable = false)
    private Integer quantity;

    // ==========================================
    // AJOUT : totalPrice (calculé : quantity × discountedPrice)
    // ==========================================
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime reservationDate;

    // ==========================================
    // AJOUT : cancelledAt (date d'annulation)
    // ==========================================
    @Column
    private LocalDateTime cancelledAt;
}