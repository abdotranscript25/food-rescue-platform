package com.foodrescue.offerservice.entity;

public enum OfferStatus {
    DRAFT,        // Brouillon (pas encore publiée par le merchant)
    PUBLISHED,    // Publiée (visibilité interne)
    AVAILABLE,    // Disponible pour réservation
    SOLD_OUT,     // Épuisée (remainingQuantity = 0)
    EXPIRED,      // Expirée (date dépassée)
    CANCELLED     // Annulée par le merchant
}