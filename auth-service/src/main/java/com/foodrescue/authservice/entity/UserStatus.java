package com.foodrescue.authservice.entity;

public enum UserStatus {
    ACTIVE,      // Compte actif, peut se connecter
    INACTIVE,    // Compte désactivé volontairement (ex: utilisateur inactif)
    SUSPENDED    // Compte suspendu par un admin (ex: comportement frauduleux)
}