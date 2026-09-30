package com.foodrescue.reservationservice.entity;

public enum ReservationStatus {
    PENDING,     // Créée, en attente de confirmation
    CONFIRMED,   // Confirmée (après décrément du stock)
    CANCELLED,   // Annulée par l'utilisateur
    COMPLETED,   // Retrait effectué par l'utilisateur
    EXPIRED      // Non retirée à temps
}