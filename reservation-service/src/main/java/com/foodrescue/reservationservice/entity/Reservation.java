package com.foodrescue.reservationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false)
    private Long userId;       // L'ID de l'utilisateur qui réserve

    @Column(nullable = false)
    private Long offerId;      // L'ID de l'offre réservée

    @Column(nullable = false)
    private Integer quantity;  // Nombre de portions réservées

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime reservationDate;

    // Enum interne
    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED
    }
}