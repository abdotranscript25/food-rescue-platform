package com.foodrescue.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaValidateRequest {

    /**
     * Token partiel reçu après le login (email + password corrects).
     * Valide 5 minutes.
     */
    private String partialToken;

    /**
     * Code à 6 chiffres saisi par l'utilisateur.
     */
    private String code;
}